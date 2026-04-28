import request from './request'

export function createCrudApi<T>(basePath: string) {
  return {
    list(): Promise<T[]> {
      return request.get(basePath)
    },
    getById(id: number): Promise<T> {
      return request.get(`${basePath}/${id}`)
    },
    create(data: Partial<T>): Promise<T> {
      return request.post(basePath, data)
    },
    update(id: number, data: Partial<T>): Promise<T> {
      return request.put(`${basePath}/${id}`, data)
    },
    remove(id: number): Promise<void> {
      return request.delete(`${basePath}/${id}`)
    }
  }
}
