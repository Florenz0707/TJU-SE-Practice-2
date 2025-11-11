package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.service.serviceInterface.TransactionService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {
    private final UserService userService;
    private final WalletService walletService;
    private final TransactionService transactionService;

    public TransactionController(
            UserService userService,
            @Qualifier("walletServiceImpl") WalletService walletServiceImpl,
            @Qualifier("transactionServiceImpl") TransactionService transactionServiceImpl) {
        this.userService = userService;
        this.walletService = walletServiceImpl;
        this.transactionService = transactionServiceImpl;
    }

    @GetMapping("/{id}")
    public HttpResult<TransactionVO> getTransactionById(
            @PathVariable("id") Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

        if (id == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");

        TransactionVO transactionVO = transactionService.getTransactionById(id);
        if (transactionVO == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Transaction NOT FOUND");
        return HttpResult.success(transactionVO);
    }

    @GetMapping("/list/{walletId}")
    public HttpResult<TransactionsRecord> getTransactionsByWalletId(
            @PathVariable Long walletId) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (walletId == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");

        User owner = walletService.getWalletOwnerById(walletId);
        if (owner == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Wallet NOT FOUND");
        if (!me.equals(owner))
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

        TransactionsRecord transactionsRecord = transactionService.getTransactionsByWalletId(walletId);
        return HttpResult.success(transactionsRecord);
    }

    @PostMapping("")
    public HttpResult<TransactionVO> createTransaction(
            @RequestBody BigDecimal amount,
            @RequestBody Integer type,
            @RequestBody Long enterWalletId,
            @RequestBody Long outWalletId) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (amount == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
        if (type == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Type CANT BE NULL");
        if (!TransactionType.isValidTransactionType(type))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Type NOT VALID");

        TransactionVO transactionVO = transactionService.createTransaction(amount, type, enterWalletId, outWalletId, me);
        if (transactionVO == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "SOMETHING IS WRONG");
        return HttpResult.success(transactionVO);
    }

    @PatchMapping("/finished")
    public HttpResult<TransactionVO> finishTransaction(
            @RequestParam Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (id == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");

        TransactionVO transactionVO = transactionService.finishTransaction(id, me);
        if (transactionVO == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "SOMETHING IS WRONG");
        return HttpResult.success(transactionVO);
    }
}
