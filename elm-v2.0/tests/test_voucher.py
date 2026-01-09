import requests
import json

BASE = "http://localhost:9000/elm"


def get_token(username, password):
    url = f"{BASE}/api/auth"
    try:
        r = requests.post(url, json={"username": username, "password": password}, timeout=5)
        r.raise_for_status()
        j = r.json()
        # possible shapes: {"id_token": "..."} or HttpResult
        if isinstance(j, dict):
            if 'id_token' in j:
                return j['id_token']
            if 'idToken' in j:
                return j['idToken']
            if 'data' in j and isinstance(j['data'], dict):
                # some auth endpoints wrap token in data
                for k in ('id_token', 'idToken', 'token'):
                    if k in j['data']:
                        return j['data'][k]
        return None
    except Exception as e:
        print('Login failed:', e)
        return None


def admin_create_public_voucher(token, threshold, value, claimable=True, valid_days=30):
    url = f"{BASE}/api/publicVoucher"
    headers = {"Authorization": f"Bearer {token}"}
    payload = {
        "threshold": str(threshold),
        "value": str(value),
        "claimable": claimable,
        "validDays": valid_days
    }
    r = requests.post(url, json=payload, headers=headers, timeout=5)
    return r


def admin_list_public_vouchers(token):
    url = f"{BASE}/api/publicVoucher/list"
    headers = {"Authorization": f"Bearer {token}"}
    r = requests.get(url, headers=headers, timeout=5)
    return r


def user_create_wallet(token):
    url = f"{BASE}/api/wallet"
    headers = {"Authorization": f"Bearer {token}"}
    r = requests.post(url, headers=headers, timeout=5)
    return r


def user_create_topup_transaction(token, amount, in_wallet_id):
    url = f"{BASE}/api/transaction"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    payload = {
        "amount": str(amount),
        "type": 0,
        "inWalletId": in_wallet_id
    }
    r = requests.post(url, json=payload, headers=headers, timeout=5)
    return r


if __name__ == '__main__':
    # Admin login
    admin_token = get_token('admin', 'admin')
    assert admin_token, 'admin token not obtained'
    print('Admin token obtained')

    # Create a public voucher
    r = admin_create_public_voucher(admin_token, 10.00, 2.00, True, 30)
    print('Create voucher status:', r.status_code, r.text)
    assert r.status_code == 200
    jr = r.json()
    assert jr.get('success') is True
    print('Public voucher created')

    # List vouchers
    r = admin_list_public_vouchers(admin_token)
    print('List vouchers status:', r.status_code)
    assert r.status_code == 200
    jr = r.json()
    assert jr.get('success') is True
    print('Available public vouchers count:', len(jr.get('data', [])))

    # User flow: login, create wallet, top-up
    user_token = get_token('user', 'password')
    assert user_token, 'user token not obtained'
    print('User token obtained')

    r = user_create_wallet(user_token)
    print('Create wallet status:', r.status_code, r.text)
    assert r.status_code == 200
    jr = r.json()
    assert jr.get('success') is True
    wallet = jr.get('data')
    wallet_id = wallet.get('id')
    print('Created wallet id:', wallet_id)

    # Create TOP_UP transaction to trigger voucher selection
    r = user_create_topup_transaction(user_token, 100.00, wallet_id)
    print('Create transaction status:', r.status_code, r.text)
    assert r.status_code == 200
    jr = r.json()
    assert jr.get('success') is True
    print('Transaction created, data:', jr.get('data'))

    # Claim a public voucher (use first public voucher id)
    data = admin_list_public_vouchers(admin_token).json().get('data', [])
    if len(data) > 0:
        pub_id = data[0].get('id')
        r = requests.post(f"{BASE}/api/privateVoucher/claim/{pub_id}", headers={"Authorization": f"Bearer {user_token}"}, timeout=5)
        print('Claim status:', r.status_code, r.text)
        assert r.status_code == 200
        jr = r.json()
        assert jr.get('success') is True

        # List user's private vouchers
        r = requests.get(f"{BASE}/api/privateVoucher/my", headers={"Authorization": f"Bearer {user_token}"}, timeout=5)
        print('List private vouchers:', r.status_code, r.text)
        assert r.status_code == 200
        jr = r.json()
        assert jr.get('success') is True
        pv_list = jr.get('data', [])
        print('Private vouchers count:', len(pv_list))
        if len(pv_list) > 0:
            pv_id = pv_list[0].get('id')
            # Redeem voucher
            r = requests.post(f"{BASE}/api/privateVoucher/redeem/{pv_id}", headers={"Authorization": f"Bearer {user_token}"}, timeout=5)
            print('Redeem status:', r.status_code, r.text)
            assert r.status_code == 200
            jr = r.json()
            assert jr.get('success') is True

    print('Voucher-related basic test finished successfully')
