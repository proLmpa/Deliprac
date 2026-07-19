interface AiConsentPopupProps {
  onConsent: (accepted: boolean) => void
}

export default function AiConsentPopup({ onConsent }: AiConsentPopupProps) {
  const respond = (accepted: boolean) => {
    localStorage.setItem('aiConsent', accepted ? 'yes' : 'no')
    window.dispatchEvent(new Event('ai-consent-changed'))
    onConsent(accepted)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" />
      <div className="relative bg-white rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6 z-10">
        <div className="text-3xl mb-3 text-center">🤖</div>
        <h2 className="text-lg font-semibold text-center mb-2">AI 추천 서비스</h2>
        <p className="text-sm text-gray-600 text-center mb-6 leading-relaxed">
          AI가 메뉴를 분석해 맞춤 추천을 드려요.
          <br />
          채팅으로 메뉴에 대해 질문할 수도 있어요.
          <br />
          AI 서비스를 이용하시겠어요?
        </p>
        <div className="flex gap-3">
          <button
            className="flex-1 px-4 py-2 text-sm rounded-xl font-medium bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
            onClick={() => respond(false)}
          >
            괜찮아요
          </button>
          <button
            className="flex-1 px-4 py-2 text-sm rounded-xl font-medium bg-orange-500 text-white hover:bg-orange-600 transition-colors"
            onClick={() => respond(true)}
          >
            사용할게요
          </button>
        </div>
      </div>
    </div>
  )
}
