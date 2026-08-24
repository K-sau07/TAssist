import '@testing-library/jest-dom/vitest'

// React 19 + Testing Library: mark this as an act() environment so RTL uses
// React's own act (react-dom/test-utils was removed in React 19).
;(globalThis as unknown as { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true
