// Takes the README screenshots by driving a locally installed Chrome (or Edge) through the Chrome DevTools
// Protocol, using only Node's built-in fetch and WebSocket (Node 22+); no extra dependencies.
//
// Usage (with the stack running, ideally on a fresh database so the numbers match the docs):
//   node scripts/take-screenshots.mjs [baseUrl] [outputDir]
//   node scripts/take-screenshots.mjs http://localhost:8088 docs/screenshots
// Set CHROME_PATH if Chrome isn't in a standard location. Ollama should be running for the AI commentary shot.

import { spawn } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const BASE_URL = process.argv[2] ?? 'http://localhost:8088'
const OUTPUT_DIR = resolve(process.argv[3] ?? 'docs/screenshots')
const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const DEBUG_PORT = 9333
const WIDTH = 1440
const HEIGHT = 900

const CHROME_CANDIDATES = [
  process.env.CHROME_PATH,
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  '/usr/bin/google-chrome',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
].filter(Boolean)

const sleep = (ms) => new Promise((done) => setTimeout(done, ms))

async function launchBrowser() {
  const executable = CHROME_CANDIDATES.find((path) => existsSync(path))
  if (!executable) throw new Error('Chrome or Edge not found; set CHROME_PATH')
  const browser = spawn(executable, [
    '--headless=new',
    `--remote-debugging-port=${DEBUG_PORT}`,
    `--user-data-dir=${mkdtempSync(join(tmpdir(), 'driverline-shots-'))}`,
    `--window-size=${WIDTH},${HEIGHT}`,
    '--hide-scrollbars',
    '--no-first-run',
    'about:blank',
  ])
  for (let attempt = 0; attempt < 50; attempt++) {
    try {
      const target = await (await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/new?about:blank`, { method: 'PUT' })).json()
      return { browser, wsUrl: target.webSocketDebuggerUrl }
    } catch {
      await sleep(200)
    }
  }
  browser.kill()
  throw new Error('Browser did not start')
}

/** Minimal CDP client: send(method, params) returns the result; once(event) waits for an event. */
function connect(wsUrl) {
  return new Promise((ready, fail) => {
    const socket = new WebSocket(wsUrl)
    let nextId = 1
    const pending = new Map()
    const listeners = []
    socket.onmessage = ({ data }) => {
      const message = JSON.parse(data)
      if (message.id && pending.has(message.id)) {
        const { done, failCall } = pending.get(message.id)
        pending.delete(message.id)
        message.error ? failCall(new Error(message.error.message)) : done(message.result)
      } else if (message.method) {
        listeners.filter((l) => l.method === message.method).forEach((l) => l.callback(message.params))
      }
    }
    socket.onerror = fail
    socket.onopen = () =>
      ready({
        send: (method, params = {}) =>
          new Promise((done, failCall) => {
            const id = nextId++
            pending.set(id, { done, failCall })
            socket.send(JSON.stringify({ id, method, params }))
          }),
        once: (method) =>
          new Promise((callback) => {
            const listener = { method, callback: (params) => { listeners.splice(listeners.indexOf(listener), 1); callback(params) } }
            listeners.push(listener)
          }),
        close: () => socket.close(),
      })
  })
}

function page(cdp) {
  const evaluate = async (expression) => {
    const { result, exceptionDetails } = await cdp.send('Runtime.evaluate', {
      expression,
      awaitPromise: true,
      returnByValue: true,
    })
    if (exceptionDetails) throw new Error(`${exceptionDetails.text}: ${expression}`)
    return result.value
  }

  const waitFor = async (expression, description, timeoutMs = 20000) => {
    const deadline = Date.now() + timeoutMs
    while (Date.now() < deadline) {
      if (await evaluate(expression)) return
      await sleep(250)
    }
    throw new Error(`Timed out waiting for ${description}`)
  }

  return {
    evaluate,
    waitFor,
    async goto(path, readyExpression, description) {
      const loaded = cdp.once('Page.loadEventFired')
      await cdp.send('Page.navigate', { url: BASE_URL + path })
      await loaded
      await waitFor(readyExpression, description)
      await sleep(300) // let layout settle
    },
    /** Clicks the first button whose accessible label or text matches exactly. */
    clickButton(label) {
      return evaluate(`(() => {
        const button = [...document.querySelectorAll('button')].find(
          (b) => b.getAttribute('aria-label') === ${JSON.stringify(label)} || b.textContent.trim() === ${JSON.stringify(label)})
        if (!button) throw new Error('No button ' + ${JSON.stringify(label)})
        button.click()
        return true
      })()`)
    },
    /** Sets a select or input value the way React expects (native setter + bubbling event). */
    setValue(selector, value) {
      return evaluate(`(() => {
        const element = document.querySelector(${JSON.stringify(selector)})
        if (!element) throw new Error('No element ' + ${JSON.stringify(selector)})
        const prototype = element instanceof HTMLSelectElement ? HTMLSelectElement.prototype : HTMLInputElement.prototype
        Object.getOwnPropertyDescriptor(prototype, 'value').set.call(element, ${JSON.stringify(value)})
        element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }))
        return true
      })()`)
    },
    async uploadFile(selector, filePath) {
      const { root } = await cdp.send('DOM.getDocument')
      const { nodeId } = await cdp.send('DOM.querySelector', { nodeId: root.nodeId, selector })
      await cdp.send('DOM.setFileInputFiles', { nodeId, files: [filePath] })
    },
    /** Saves a screenshot of the region from the top of `fromSelector` to the bottom of `toSelector`. */
    async screenshot(fileName, fromSelector = 'body', toSelector = fromSelector, padding = 16) {
      const box = await evaluate(`(() => {
        const top = document.querySelector(${JSON.stringify(fromSelector)}).getBoundingClientRect()
        const bottom = document.querySelector(${JSON.stringify(toSelector)}).getBoundingClientRect()
        return { y: top.top + window.scrollY, bottom: bottom.bottom + window.scrollY }
      })()`)
      const y = Math.max(0, box.y - padding)
      const { data } = await cdp.send('Page.captureScreenshot', {
        format: 'png',
        captureBeyondViewport: true,
        clip: { x: 0, y, width: WIDTH, height: Math.ceil(box.bottom - y + padding), scale: 1 },
      })
      writeFileSync(join(OUTPUT_DIR, fileName), Buffer.from(data, 'base64'))
      console.log(`saved ${fileName}`)
    },
  }
}

const textPresent = (text) => `document.body.innerText.includes(${JSON.stringify(text)})`

async function main() {
  mkdirSync(OUTPUT_DIR, { recursive: true })
  const { browser, wsUrl } = await launchBrowser()
  const cdp = await connect(wsUrl)
  try {
    await cdp.send('Page.enable')
    await cdp.send('Runtime.enable')
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: WIDTH, height: HEIGHT, deviceScaleFactor: 1, mobile: false })
    const app = page(cdp)

    // 1. Variance by department with the Fleet & Equipment drill-down open.
    await app.goto('/variance', textPresent('Fleet & Equipment'), 'variance report')
    await app.clickButton('Show accounts for Fleet & Equipment')
    await app.waitFor(textPresent('5300 Vehicle & Fuel'), 'drill-down rows')
    await app.screenshot('variance-drill-down.png', '.app-header', 'table.report')

    // 2. AI commentary (written by the local language model when Ollama is running).
    await app.clickButton('Generate commentary')
    await app.waitFor(`!!document.querySelector('.commentary-source')`, 'commentary', 180000)
    await app.screenshot('ai-commentary.png', 'section.commentary')

    // 3. Monthly trend for Vehicle & Fuel (from the drill-down's Trend button).
    await app.clickButton('Monthly trend for 5300 Vehicle & Fuel in Fleet & Equipment')
    await app.waitFor(textPresent('Closed months total'), 'monthly trend')
    await app.screenshot('variance-trend.png', '#variance-heading', 'table.report')

    // 4. Forecast: scenario cards and the 12-month table.
    await app.goto('/forecast', `!!document.querySelector('.forecast-table') && ${textPresent('Worst')}`, 'forecast')
    await app.screenshot('forecast-scenarios.png', '#forecast-heading', '.forecast-table')

    // 5. Driver editor with an unsaved change, showing the live preview.
    await app.setValue('input[aria-label="Contract Cleaning Revenue, Sales & Account Management, Base, Active contracts"]', '64')
    await app.waitFor(textPresent('Save drivers (1)'), 'driver change')
    await app.screenshot('forecast-drivers.png', 'section.drivers', 'section.drivers .actions')

    // 6. Budget entry for Operations, with one edited (yellow) and one invalid (red) cell.
    await app.goto('/budgets', textPresent('Budget entry'), 'budget page')
    await app.setValue('label select', '2026')
    await app.evaluate(`(() => {
      const select = [...document.querySelectorAll('label')].find((l) => l.textContent.startsWith('Department')).querySelector('select')
      Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value').set.call(select, 'OPS')
      select.dispatchEvent(new Event('change', { bubbles: true }))
      return true
    })()`)
    await app.waitFor(`!!document.querySelector('input[aria-label="Cleaning Supplies, Operations, Jan"]')`, 'Operations grid')
    await app.setValue('input[aria-label="Cleaning Supplies, Operations, Jan"]', '24,500')
    await app.setValue('input[aria-label="Cleaning Supplies, Operations, Feb"]', '12.345')
    await app.screenshot('budget-entry.png', '#budget-heading', '.actions')

    // 7. Actuals import rejecting a file with mistakes, listing every row error.
    await app.goto('/actuals/import', textPresent('Import actuals'), 'import page')
    await app.uploadFile('input[type=file]', join(REPO_ROOT, 'samples', 'actuals-with-errors.csv'))
    await app.waitFor(`!document.querySelector('button[type=submit]').disabled`, 'file selected')
    await app.clickButton('Import')
    await app.waitFor(`!!document.querySelector('.errors-table')`, 'import errors')
    await app.screenshot('import-errors.png', '#import-heading', '.message-error')
  } finally {
    cdp.close()
    browser.kill()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
