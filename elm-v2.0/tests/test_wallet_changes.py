import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


def test_schema_has_wallet_columns():
    sql = read('src/main/resources/schema.sql')
    assert re.search(r"create table\s+wallet", sql, re.IGNORECASE), "schema.sql should define wallet table"
    assert 'credit_limit' in sql, "schema.sql must contain credit_limit column"
    assert 'last_withdrawal_at' in sql, "schema.sql must contain last_withdrawal_at column"
    assert 'owner_id' in sql, "schema.sql must contain owner_id fk"


def test_wallet_java_has_fields_and_method():
    code = read('src/main/java/cn/edu/tju/elm/model/BO/Wallet.java')
    assert 'creditLimit' in code, 'Wallet.java should contain creditLimit field'
    assert 'lastWithdrawalAt' in code, 'Wallet.java should contain lastWithdrawalAt field'
    assert 'decBalanceWithCredit' in code, 'Wallet.java should contain decBalanceWithCredit method'


def test_transaction_exception_constants():
    code = read('src/main/java/cn/edu/tju/elm/exception/TransactionException.java')
    assert 'WITHDRAWAL_COOLDOWN' in code
    assert 'WITHDRAWAL_MINIMUM' in code


def test_transaction_service_withdrawal_and_payment_fix():
    code = read('src/main/java/cn/edu/tju/elm/service/serviceImpl/TransactionServiceImpl.java')
    assert 'MIN_WITHDRAWAL' in code
    assert 'WITHDRAWAL_COOLDOWN_SECONDS' in code
    assert 'decBalanceWithCredit' in code
    assert re.search(r"inWallet\.addBalance\(|inWallet.addBalance\s*\(", code), 'finishTransaction should credit inWallet'
