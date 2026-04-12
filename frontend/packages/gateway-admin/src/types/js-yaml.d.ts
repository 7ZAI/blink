declare module 'js-yaml' {
  function dump(obj: any, options?: { indent?: number; lineWidth?: number }): string
  function load(str: string): any
  function safeLoad(str: string): any
  function safeDump(obj: any, options?: { indent?: number; lineWidth?: number }): string
}
