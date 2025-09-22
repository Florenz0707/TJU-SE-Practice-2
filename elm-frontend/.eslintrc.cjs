// .eslintrc.cjs
module.exports = {
    root: true,
    env: {
      browser: true,
      es2021: true,
      node: true,
    },
    extends: [
      'eslint:recommended',
      'plugin:vue/vue3-recommended', // 使用 vue3 的推荐规则
      'plugin:@typescript-eslint/recommended',
      'plugin:prettier/recommended', // Prettier 插件
    ],
    parser: 'vue-eslint-parser',
    parserOptions: {
      ecmaVersion: 'latest',
      parser: '@typescript-eslint/parser',
      sourceType: 'module',
    },
    plugins: ['vue', '@typescript-eslint'],
    rules: {
      // 在这里可以覆盖或添加自定义规则
      'vue/multi-word-component-names': 'off', // 暂时关闭组件名必须多单词的规则
    },
  };