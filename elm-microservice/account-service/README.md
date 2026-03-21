# account-service

阶段3拆分准备骨架（钱包/交易/券域）。

## 当前状态

- 工程骨架已创建，可独立编译。
- 业务代码迁移尚未开始。

## 下一步迁移范围

- Controller: WalletController / TransactionController / PublicVoucherController / PrivateVoucherController
- Service: WalletService / TransactionService / PublicVoucherService / PrivateVoucherService
- Repository: WalletRepository / TransactionRepository / PublicVoucherRepository / PrivateVoucherRepository
- Model: Wallet / Transaction / PublicVoucher / PrivateVoucher + 对应 VO/RECORD/异常/常量

## 启动

`mvn spring-boot:run`
