# Sample files

Example actuals files for the fictional demo company (Ace Janitorial Service). The seeded demo data has actuals through September 2026, so these add October 2026.

| File | What it shows |
| --- | --- |
| `actuals-2026-10.csv` | A valid month of actuals (15 rows) |
| `actuals-2026-10.xlsx` | The same data as an Excel workbook with numeric cells |
| `actuals-with-errors.csv` | A file with six deliberate mistakes (month 13, unknown codes, 3 decimals, duplicate row, missing amount). Importing it is rejected and lists every problem |

Import them from the **Import actuals** page, or from PowerShell with the backend running:

```powershell
curl.exe -F "file=@samples/actuals-2026-10.csv" http://localhost:8080/api/actuals/import
```

Importing the same month again updates the amounts instead of creating duplicates.
