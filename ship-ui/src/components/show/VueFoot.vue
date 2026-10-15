<template>
  <footer class="vue-foot">
    <!-- 波浪动画 -->
    <div class="foot-wave">
      <svg class="waves" preserveAspectRatio="none" shape-rendering="auto"
           viewBox="0 24 150 28" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
        <defs>
          <path id="gentle-wave"
                d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z"/>
        </defs>
        <g class="parallax">
          <use fill="rgba(30,111,168,0.16)" x="48" xlink:href="#gentle-wave" y="0"/>
          <use fill="rgba(30,111,168,0.10)" x="48" xlink:href="#gentle-wave" y="3"/>
          <use fill="rgba(15,138,126,0.08)" x="48" xlink:href="#gentle-wave" y="5"/>
          <use fill="rgba(248,248,255)" x="48" xlink:href="#gentle-wave" y="7"/>
        </g>
      </svg>
    </div>

    <!-- 页脚内容 -->
    <div class="foot-main">
      <div class="foot-cols">
        <!-- 品牌 -->
        <div class="foot-col foot-brand">
          <div class="brand-line">
            <span class="brand-mark">PET</span>
            <span class="brand-name">mixpet</span>
          </div>
          <p class="brand-desc">宠物医院管理系统</p>
        </div>

        <!-- 快捷入口 -->
        <div class="foot-col">
          <h3 class="col-title">快捷入口</h3>
          <ul class="col-list">
            <li><router-link to="/reception/doctorSee">医生团队</router-link></li>
            <li><router-link to="/reception/petSee">待领养宠物</router-link></li>
            <li><router-link to="/reception/chat">在线咨询</router-link></li>
          </ul>
        </div>

        <!-- 关于 -->
        <div class="foot-col">
          <h3 class="col-title">关于我们</h3>
          <ul class="col-list">
            <li>营业时间：09:00 - 20:00</li>
            <li>咨询电话：400-000-0000</li>
            <li>地址：请在后台设置</li>
          </ul>
        </div>
      </div>

      <div class="foot-bottom">
        <span>© 2026 mixpet</span>
        <span class="dot">·</span>
        <span>基于 <a href="https://github.com/MixJade/MixPet" target="_blank" rel="noopener">MixPet</a> 二次开发</span>
      </div>
    </div>
  </footer>
</template>

<script lang="ts" setup>
</script>

<style lang="scss" scoped>
.vue-foot {
  width: 100%;
  margin-top: 2.5rem;
  background-color: #F8F8FF;
}

/* 波浪 */
.foot-wave .waves {
  position: relative;
  width: 100%;
  height: 12vh;
  margin-bottom: -7px;
  min-height: 80px;
  max-height: 130px;

  @media (max-width: 768px) {
    height: 40px;
    min-height: 40px;
  }

  .parallax > use {
    animation: move-forever 25s cubic-bezier(.55, .5, .45, .5) infinite;

    &:nth-child(1) { animation-delay: -2s; animation-duration: 7s; }
    &:nth-child(2) { animation-delay: -3s; animation-duration: 10s; }
    &:nth-child(3) { animation-delay: -4s; animation-duration: 13s; }
    &:nth-child(4) { animation-delay: -5s; animation-duration: 20s; }
  }

  @keyframes move-forever {
    0% { transform: translate3d(-90px, 0, 0) }
    100% { transform: translate3d(85px, 0, 0) }
  }
}

/* 主体 */
.foot-main {
  border-top: 1px solid #E4E7ED;
  padding: 36px 24px 24px;
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
}

.foot-cols {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr;
  gap: 32px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

.foot-col {
  text-align: left;
}

.foot-brand .brand-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  .brand-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    background: #1E6FA8;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
  }

  .brand-name {
    font-size: 16px;
    font-weight: 700;
    color: #1E6FA8;
    letter-spacing: 0.3px;
  }
}

.brand-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.col-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1F2D3D;
}

.col-list {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;

    a {
      color: #606266;
      transition: color 0.15s;

      &:hover {
        color: #1E6FA8;
      }
    }
  }
}

/* 底栏 */
.foot-bottom {
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid #E4E7ED;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;

  a {
    color: #1E6FA8;

    &:hover {
      text-decoration: underline;
    }
  }

  .dot {
    color: #C0C4CC;
  }
}
</style>