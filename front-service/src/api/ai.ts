import client from './client'

export interface RecommendedItem {
  productName: string
  reason: string
}

export interface RecommendResponse {
  recommendations: RecommendedItem[]
  showPreferencePicker: boolean
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatResponse {
  reply: string
}

export const getRecommendations = (storeId: number, categoryPreferences: string[] = []) =>
  client.post<RecommendResponse>('/api/ai/recommend', { storeId, categoryPreferences }).then((r) => r.data)

export const sendChat = (messages: ChatMessage[]) =>
  client.post<ChatResponse>('/api/ai/chat', { messages }).then((r) => r.data)
