package cn.edu.tju.elm.model.RECORD;

import cn.edu.tju.elm.model.VO.TransactionVO;

import java.util.List;

public record TransactionsRecord(List<TransactionVO> enterTransactions, List<TransactionVO> outTransactions) {
}
