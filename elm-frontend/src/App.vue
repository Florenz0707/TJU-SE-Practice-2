<template>
  <router-view />
</template>

<script setup lang="ts">
import { watch } from 'vue';
import { useCartStore, type AnimationOrigin } from './store/cart';

const cartStore = useCartStore();

const triggerFlyAnimation = (origin: AnimationOrigin) => {
  const cartIconEl = cartStore.cartIconElement;
  if (!origin || !cartIconEl) return;

  const targetRect = cartIconEl.getBoundingClientRect();
  const targetX = targetRect.left + targetRect.width / 2;
  const targetY = targetRect.top + targetRect.height / 2;

  const flyingEl = document.createElement('img');
  flyingEl.src = origin.imgSrc;
  flyingEl.style.position = 'fixed';
  flyingEl.style.left = `${origin.x}px`;
  flyingEl.style.top = `${origin.y}px`;
  flyingEl.style.width = '50px';
  flyingEl.style.height = '50px';
  flyingEl.style.borderRadius = '50%';
  flyingEl.style.objectFit = 'cover';
  flyingEl.style.zIndex = '9999';
  flyingEl.style.pointerEvents = 'none';
  flyingEl.style.transform = 'translate(-50%, -50%)';

  document.body.appendChild(flyingEl);

  flyingEl.animate([
    { transform: `translate(-50%, -50%) scale(1)`, opacity: 1 },
    { transform: `translate(${targetX - origin.x}px, ${targetY - origin.y}px) scale(0.1)`, opacity: 0.5 }
  ], {
    duration: 800,
    easing: 'cubic-bezier(0.3, 0, 0.7, -0.3)',
  }).onfinish = () => {
    document.body.removeChild(flyingEl);
    cartStore.animationOrigin = null; // Reset after animation
  };
};

watch(() => cartStore.animationOrigin, (newOrigin) => {
  if (newOrigin) {
    triggerFlyAnimation(newOrigin);
  }
});
</script>
