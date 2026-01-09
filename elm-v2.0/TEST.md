# Tests

## Java unit tests

Run the project's unit tests with Maven:

```bash
mvn test
```

The added tests include `TransactionServiceImplTest` which uses Mockito to mock repositories and verifies PAYMENT create/finish behavior and a concurrency simulation.

## Python quick checks

There are simple Python tests under `tests/` that verify presence of schema changes and source modifications. Run them with:

```bash
python -m pytest -q
```
