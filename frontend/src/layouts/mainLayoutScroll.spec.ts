import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('./MainLayout.vue', import.meta.url), 'utf8')

assert.match(
  source,
  /\.layout-main__inner\s*\{[\s\S]*overflow-y:\s*auto;/,
  'layout-main__inner should provide its own vertical scrollbar'
)
