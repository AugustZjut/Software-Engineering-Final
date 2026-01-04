import { createApp, reactive } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import $ from 'jquery'
// import 'babel-polyfill'; // Not needed in Vue 3

import api from './api/index.js';

const app = createApp(App);

app.config.globalProperties.$api = api;

let globalData = reactive({
    userInfo: {
        nickname: ''
    }
});
let sta = reactive({
    isLogin: false,
    adminName: ''
});

app.config.globalProperties.$sta = sta;
app.config.globalProperties.$globalData = globalData;

app.use(ElementPlus, {
    size: 'default'
});

router.beforeEach((to, from, next) => {
    document.title = `${to.meta.title}`;
    // console.log(to.path,'userInfo:', globalData.userInfo);
    const nickname = globalData.userInfo.nickname;
    if (!nickname
        && (to.path === '/me'
            || to.path === '/message'
            || to.path === '/release'
            || to.path === '/order')) {
        api.getUserInfo().then(res => {
            console.log('getUserInfo:', res);
            if (res.status_code !== 1) {
                next('/login');
            } else {
                if (res.data.signInTime) {
                    res.data.signInTime = res.data.signInTime.substring(0, 10);
                }
                // Object.assign to keep reactivity if we replaced the whole object, 
                // but userInfo is a property so direct assignment is fine with reactive()
                globalData.userInfo = res.data;
                next();
            }
        }).catch(e => {
            next('/login');
        });

    } else {
        next();
    }
});

app.use(router);
app.mount('#app');
