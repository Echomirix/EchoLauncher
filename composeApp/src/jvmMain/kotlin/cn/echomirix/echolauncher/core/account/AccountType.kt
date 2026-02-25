package cn.echomirix.echolauncher.core.account

enum class AccountType {
    OFFLINE, // 离线账号，输入任意名字都行
    LITTLESKIN, // LittleSkin 账号，使用 LittleSkin API 登录
    MICROSOFT, // Microsoft 账号，使用微软 OAuth 登录
}