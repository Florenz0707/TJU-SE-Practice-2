import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import vue from "eslint-plugin-vue";
import vueParser from "vue-eslint-parser";
import prettier from "eslint-config-prettier";

export default [
  js.configs.recommended,

  ...tseslint.configs.recommended,

  ...vue.configs["flat/recommended"],

  {
    ignores: ["**/node_modules/**", "**/dist/**"],
  },

  {
    files: ["**/*.vue"],

    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: "@typescript-eslint/parser",
        ecmaVersion: "latest",
        sourceType: "module",
      },
    },
  },

  {
    files: ["**/*.{js,ts,vue}"],

    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },

    rules: {
      "no-unused-vars": "warn",
      "no-debugger": "warn",
      "prefer-const": "warn",

      // Vue 常见规则调整
      "vue/multi-word-component-names": "off",
      "vue/no-v-html": "off",
    },
  },

  prettier,
];
