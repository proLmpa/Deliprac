import type { HTMLAttributes } from 'react'

interface CardProps extends HTMLAttributes<HTMLDivElement> {}

export default function Card({ className = '', children, ...props }: CardProps) {
  return (
    <div
      className={`bg-white rounded-lg shadow border border-gray-100 p-4 ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
