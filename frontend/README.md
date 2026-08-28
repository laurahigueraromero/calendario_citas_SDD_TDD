# frontend — calendario_citas_SDD_TDD

SPA en Vue 3 + Vite. Gestor de paquetes: **pnpm** (ver campo `packageManager` en `package.json`; con Corepack basta `corepack enable`).

## Puesta en marcha

```sh
pnpm install
```

### Desarrollo con recarga en caliente

```sh
pnpm dev
```

### Build de producción

```sh
pnpm build
```

### Tests unitarios ([Vitest](https://vitest.dev/))

```sh
pnpm test:unit          # una pasada (lo que ejecuta el CI)
pnpm test:unit:watch    # modo watch
```

### Lint ([oxlint](https://oxc.rs/) + [ESLint](https://eslint.org/))

```sh
pnpm lint       # comprueba, sin modificar (lo que ejecuta el CI)
pnpm lint:fix   # aplica correcciones automáticas
```

## IDE recomendado

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (deshabilita Vetur).

Configuración de Vite: [Vite Configuration Reference](https://vite.dev/config/).
