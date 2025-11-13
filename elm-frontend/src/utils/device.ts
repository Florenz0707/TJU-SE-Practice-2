/**
 * Simple device detection based on user agent.
 * @returns {boolean} - True if the user agent string matches common mobile patterns.
 */
export const isMobile = (breakpoint: number = 768): boolean => {
  // SSR (服务器端渲染) 安全性检查：
  // 确保 window 和 navigator 对象存在，否则在 Node.js 环境下会报错。
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return false;
  }

  // 1. 检查 User Agent (原始逻辑)
  const isUserAgentMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

  // 2. 检查窗口宽度
  const isWindowSizeMobile = window.innerWidth < breakpoint;

  // 如果任一条件为真，则返回 true
  return isUserAgentMobile || isWindowSizeMobile;
};