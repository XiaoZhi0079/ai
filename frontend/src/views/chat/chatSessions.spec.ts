import assert from 'node:assert/strict'
import {
  appendSessionChunk,
  beginSessionRequest,
  ensureChatSession,
  finishSessionRequest,
  isChatSessionBusy,
  type ChatSessionMap
} from './chatSessions.ts'

type TestMessage = { content: string }

function testBusyStateStaysInsideItsOwnChatSession() {
  const sessions: ChatSessionMap<TestMessage> = {}
  const firstSession = ensureChatSession(sessions, 'chat-1')
  const secondSession = ensureChatSession(sessions, 'chat-2')

  beginSessionRequest(firstSession, 'sending', 1)

  assert.equal(isChatSessionBusy(firstSession), true)
  assert.equal(isChatSessionBusy(secondSession), false)
}

function testStreamChunksOnlyUpdateTheOriginatingChatSession() {
  const sessions: ChatSessionMap<TestMessage> = {}
  const firstSession = ensureChatSession(sessions, 'chat-1')
  const secondSession = ensureChatSession(sessions, 'chat-2')

  firstSession.messages.push({ content: '' })
  secondSession.messages.push({ content: 'keep' })
  beginSessionRequest(firstSession, 'sending', 9)

  const appended = appendSessionChunk(firstSession, 0, 'hello', 9)

  assert.equal(appended, true)
  assert.equal(firstSession.messages[0]?.content, 'hello')
  assert.equal(secondSession.messages[0]?.content, 'keep')
}

function testStaleRequestCallbacksAreIgnoredAfterASessionRequestChanges() {
  const sessions: ChatSessionMap<TestMessage> = {}
  const session = ensureChatSession(sessions, 'chat-1')

  session.messages.push({ content: '' })
  beginSessionRequest(session, 'sending', 1)
  finishSessionRequest(session, 'sending', 1)
  beginSessionRequest(session, 'sending', 2)

  const appended = appendSessionChunk(session, 0, 'old', 1)

  assert.equal(appended, false)
  assert.equal(session.messages[0]?.content, '')
}

testBusyStateStaysInsideItsOwnChatSession()
testStreamChunksOnlyUpdateTheOriginatingChatSession()
testStaleRequestCallbacksAreIgnoredAfterASessionRequestChanges()
