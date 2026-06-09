export function normalizeImageUrl(url) {
  const value = String(url || '').trim()
  if (!value) return ''
  if (/^(https?:)?\/\//.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }
  return value.startsWith('/') ? value : `/${value}`
}

export function normalizeImageUrls(urls) {
  if (!Array.isArray(urls)) return []
  return urls.map(normalizeImageUrl).filter(Boolean)
}
