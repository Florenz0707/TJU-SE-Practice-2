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

        try {
            TransactionVO transactionVO = transactionService.getTransactionById(id);
            return HttpResult.success(transactionVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
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

        try {
            TransactionsRecord transactionsRecord = transactionService.getTransactionsByWalletId(walletId);
            return HttpResult.success(transactionsRecord);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("")
    public HttpResult<TransactionVO> createTransaction(
            @RequestBody TransactionVO transactionVO) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

        if (transactionVO == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "TransactionVO CANT BE NULL");
        if (transactionVO.getAmount() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
        if (transactionVO.getType() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Type CANT BE NULL");
        if (!TransactionType.isValidTransactionType(transactionVO.getType()))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Type NOT VALID");

        if (transactionVO.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Amount NOT VALID");
        if (transactionVO.getType().equals(TransactionType.PAYMENT)) {
            if (transactionVO.getInWalletId() == null)
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "InWalletId CANT BE NULL");
            if (transactionVO.getOutWalletId() == null)
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OutWalletId CANT BE NULL");
        } else if (transactionVO.getType().equals(TransactionType.TOP_UP) && transactionVO.getInWalletId() == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "InWalletId CANT BE NULL");
        } else if (transactionVO.getType().equals(TransactionType.WITHDRAW) && transactionVO.getOutWalletId() == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OutWalletId CANT BE NULL");
        }

        try {
            TransactionVO retTransactionVO = transactionService.createTransaction(
                    transactionVO.getAmount(), transactionVO.getType(),
                    transactionVO.getInWalletId(), transactionVO.getOutWalletId());
            return HttpResult.success(retTransactionVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
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

        try {
            TransactionVO transactionVO = transactionService.finishTransaction(id, me);
            return HttpResult.success(transactionVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }
}
