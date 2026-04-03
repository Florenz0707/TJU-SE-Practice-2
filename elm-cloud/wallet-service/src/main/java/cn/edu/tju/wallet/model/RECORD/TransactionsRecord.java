package cn.edu.tju.wallet.model.RECORD;

import cn.edu.tju.wallet.model.VO.TransactionVO;
import java.util.List;

public record TransactionsRecord(
    List<TransactionVO> inTransactions, List<TransactionVO> outTransactions) {}
