export interface ChatSessionState<TMessage = unknown, TImage = unknown> {
  messages: TMessage[]
  uploadedImages: TImage[]
  sending: boolean
  generatingImage: boolean
  loaded: boolean
  activeRequestId?: number
}

export type ChatSessionMap<TMessage = unknown, TImage = unknown> = Record<string, ChatSessionState<TMessage, TImage>>

export interface SessionMessageLike {
  content: string
}

export function createEmptyChatSession<TMessage = unknown, TImage = unknown>(): ChatSessionState<TMessage, TImage> {
  return {
    messages: [],
    uploadedImages: [],
    sending: false,
    generatingImage: false,
    loaded: false
  }
}

export function ensureChatSession<TMessage = unknown, TImage = unknown>(
  sessions: ChatSessionMap<TMessage, TImage>,
  chatId: string
): ChatSessionState<TMessage, TImage> {
  if (!sessions[chatId]) {
    sessions[chatId] = createEmptyChatSession<TMessage, TImage>()
  }

  return sessions[chatId]
}

export function isChatSessionBusy<TMessage = unknown, TImage = unknown>(session?: ChatSessionState<TMessage, TImage>) {
  return Boolean(session?.sending || session?.generatingImage)
}

export function beginSessionRequest<TMessage = unknown, TImage = unknown>(
  session: ChatSessionState<TMessage, TImage>,
  requestType: 'sending' | 'generatingImage',
  requestId: number
) {
  session[requestType] = true
  session.activeRequestId = requestId
}

export function finishSessionRequest<TMessage = unknown, TImage = unknown>(
  session: ChatSessionState<TMessage, TImage> | undefined,
  requestType: 'sending' | 'generatingImage',
  requestId: number
) {
  if (!session || session.activeRequestId !== requestId) {
    return false
  }

  session[requestType] = false
  if (!session.sending && !session.generatingImage) {
    session.activeRequestId = undefined
  }
  return true
}

export function appendSessionChunk<TMessage extends SessionMessageLike, TImage = unknown>(
  session: ChatSessionState<TMessage, TImage> | undefined,
  messageIndex: number,
  chunk: string,
  requestId?: number
) {
  if (!session) {
    return false
  }

  if (requestId != null && session.activeRequestId !== requestId) {
    return false
  }

  const message = session.messages[messageIndex]
  if (!message) {
    return false
  }

  message.content += chunk
  return true
}
