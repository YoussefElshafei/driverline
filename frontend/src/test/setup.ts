import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Unmount rendered components between tests so one test's DOM can't leak into the next.
afterEach(() => {
  cleanup()
})
