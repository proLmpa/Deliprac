type Status = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SOLD' | 'CANCELED'

const colorMap: Record<Status, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  INACTIVE: 'bg-gray-100 text-gray-500',
  PENDING: 'bg-yellow-100 text-yellow-700',
  SOLD: 'bg-blue-100 text-blue-700',
  CANCELED: 'bg-red-100 text-red-600',
}

export default function StatusBadge({ status }: { status: Status }) {
  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${colorMap[status]}`}>
      {status}
    </span>
  )
}
