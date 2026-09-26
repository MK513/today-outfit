// 백엔드 API 공통 클라이언트.
// - 요청 본문은 camelCase → snake_case, 응답은 snake_case → camelCase로 변환한다.
// - 실패 응답은 명세의 Error 스키마({ code, error_code, message })를 ApiError로 던진다.

const BASE_URL = '/api'
const NETWORK_ERROR_MESSAGE = '서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.'

let getToken = () => null
let handleUnauthorized = () => {}

/**
 * main.js에서 한 번 호출한다.
 * @param {{ getToken: () => string | null, onUnauthorized: () => void }} options
 */
export function configureApi(options) {
  getToken = options.getToken
  handleUnauthorized = options.onUnauthorized
}

export class ApiError extends Error {
  constructor(status, errorCode, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
  }
}

const toSnakeKey = (key) => key.replace(/[A-Z]/g, (c) => `_${c.toLowerCase()}`)
const toCamelKey = (key) => key.replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase())

function convertKeys(value, convert) {
  if (Array.isArray(value)) return value.map((v) => convertKeys(v, convert))
  if (value && typeof value === 'object' && value.constructor === Object) {
    return Object.fromEntries(Object.entries(value).map(([k, v]) => [convert(k), convertKeys(v, convert)]))
  }
  return value
}

async function request(method, path, body) {
  const token = getToken()
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(convertKeys(body, toSnakeKey)),
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', NETWORK_ERROR_MESSAGE)
  }

  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    // 토큰을 보냈는데 401이면 만료·무효화된 세션이다. (로그인 실패 401과 구분)
    if (response.status === 401 && token) handleUnauthorized()
    throw new ApiError(
      response.status,
      data?.error_code ?? 'UNKNOWN',
      data?.message ?? '요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.',
    )
  }
  return convertKeys(data, toCamelKey)
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  delete: (path) => request('DELETE', path),
}
