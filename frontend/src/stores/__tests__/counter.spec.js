import { beforeEach, describe, expect, it } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCounterStore } from '../counter'

// Patrón de referencia para testear un store de Pinia: se crea una instancia
// nueva de Pinia por test para que el estado no se comparta entre casos.
describe('counter store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('parte de cero', () => {
    const store = useCounterStore()
    expect(store.count).toBe(0)
    expect(store.doubleCount).toBe(0)
  })

  it('increment suma uno y doubleCount se recalcula', () => {
    const store = useCounterStore()
    store.increment()
    store.increment()
    expect(store.count).toBe(2)
    expect(store.doubleCount).toBe(4)
  })
})
