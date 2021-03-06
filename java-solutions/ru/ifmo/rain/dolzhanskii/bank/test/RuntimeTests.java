package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteBank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

abstract class RuntimeTests extends CommonTests {

    static final String TEST_FIRST_NAME = "John";
    static final String TEST_LAST_NAME = "Smith";
    static final String TEST_PASSPORT = "1111 111111";
    static final String TEST_SUB_ID = "ALPHA";
    static final String TEST_ACCOUNT_ID = "test";
    private static final int TEST_AMOUNT_DELTA = 100;

    @BeforeAll
    static void beforeAll() throws RemoteException {
        safeCreateRegistry();
    }

    @BeforeEach
    void beforeEach() throws RemoteException, MalformedURLException {
        bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        Naming.rebind(RemoteCredentials.getBankUrl(), bank);
    }

    @AfterEach
    void afterEach() throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(bank, false);
    }

    static Account safeCreateRemoteAccount(final String id) throws RemoteException {
        final Account account = bank.createAccount(id);
        assertNotNull(account);
        return account;
    }

    static Account safeGetLocalAccount() throws RemoteException {
        final Account account = bank.getLocalAccount(RuntimeTests.TEST_ACCOUNT_ID);
        assertNotNull(account);
        return account;
    }

    private static void safeCreateMultipleAccounts(final List<String> ids) throws RemoteException {
        try {
            ids.forEach(id -> {
                try {
                    bank.createAccount(id);
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Remote exception occurred", e);
        }
    }

    static void validateLocalAndRemoteBehavior(final Account remoteAccount, final Account localAccount1,
                                                       final Account localAccount2) throws RemoteException {
        remoteAccount.setAmount(TEST_AMOUNT_DELTA);

        assertEquals(TEST_AMOUNT_DELTA, remoteAccount.getAmount());
        assertEquals(0, localAccount1.getAmount());
        assertEquals(0, localAccount2.getAmount());

        localAccount1.setAmount(2 * TEST_AMOUNT_DELTA);

        assertEquals(TEST_AMOUNT_DELTA, remoteAccount.getAmount());
        assertEquals(2 * TEST_AMOUNT_DELTA, localAccount1.getAmount());
        assertEquals(0, localAccount2.getAmount());

        localAccount2.setAmount(3 * TEST_AMOUNT_DELTA);

        assertEquals(TEST_AMOUNT_DELTA, remoteAccount.getAmount());
        assertEquals(2 * TEST_AMOUNT_DELTA, localAccount1.getAmount());
        assertEquals(3 * TEST_AMOUNT_DELTA, localAccount2.getAmount());
    }

    static void multiThreadAccountQueries(final int countOfThreads, final int requestsPerItem,
                                          final int countOfAccounts) throws InterruptedException, RemoteException {
        final List<Integer> deltas = IntStream.range(0, countOfAccounts).boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        final List<String> ids = generateTestIds(countOfAccounts);
        safeCreateMultipleAccounts(ids);

        multiThreadBase(countOfThreads, requestsPerItem, 1, countOfAccounts, (i, j) -> {
            try {
                final Account account = safeGetRemoteAccount(ids.get(j));
                account.addAmount(deltas.get(j));
            } catch (final RemoteException e) {
                throw new UncheckedIOException(e);
            }
        });

        validateAccountAmounts(countOfAccounts, ids, i -> deltas.get(i) * requestsPerItem);
    }

    static Person safeCreatePerson() throws RemoteException {
        final Person person = bank.createPerson(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_PASSPORT);
        assertNotNull(person);
        return person;
    }

    static Account safeAddLinkedAccount(final Person person) throws RemoteException {
        final Account account = person.createLinkedAccount(TEST_SUB_ID);
        assertNotNull(account);
        return account;
    }

    static Account safeGetLinkedAccount(final Person person) throws RemoteException {
        final Account account = person.getLinkedAccount(TEST_SUB_ID);
        assertNotNull(account);
        return account;
    }

    static Account safeCreatePersonWithLinkedAccount() throws RemoteException {
        final Person person = safeCreatePerson();
        final Account account = safeAddLinkedAccount(person);
        account.setAmount(account.getAmount() + TEST_AMOUNT_DELTA);
        return account;
    }

    static void validateDefaultPerson(final Person person) throws RemoteException {
        assertNotNull(person);
        assertEquals(TEST_FIRST_NAME, person.getFirstName());
        assertEquals(TEST_LAST_NAME, person.getLastName());
        assertEquals(TEST_PASSPORT, person.getPassport());
    }

    static void validateAccountsSync(final Account account1, final Account account2)
            throws RemoteException {
        assertNotNull(account1);
        assertNotNull(account2);

        assertEquals(TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(TEST_AMOUNT_DELTA, account2.getAmount());

        account2.setAmount(account2.getAmount() + TEST_AMOUNT_DELTA);
        assertEquals(2 * TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals( 2 * TEST_AMOUNT_DELTA, account2.getAmount());
    }

    static void validateAccountsDesync(final Account account1, final Account account2) throws RemoteException {
        assertNotNull(account1);
        assertNotNull(account2);

        assertEquals(TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(TEST_AMOUNT_DELTA, account2.getAmount());

        account1.setAmount(2 * TEST_AMOUNT_DELTA);
        assertEquals(2 * TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(TEST_AMOUNT_DELTA, account2.getAmount());

        account2.setAmount(3 * TEST_AMOUNT_DELTA);
        assertEquals(2 * TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(3 * TEST_AMOUNT_DELTA, account2.getAmount());
    }

    private static void safeCreateMultiplePersonsWithMultipleAccounts(final List<String> passports,
                                                                      final List<String> subIds)
            throws RemoteException {
        final RemoteException exception = new RemoteException();

        try {
            passports.forEach(passport -> {
                try {
                    final Person person = bank.createPerson(TEST_FIRST_NAME, TEST_LAST_NAME, passport);
                    assertNotNull(person);
                    subIds.forEach(subId -> {
                        try {
                            assertNotNull(person.createLinkedAccount(subId));
                        } catch (final RemoteException e) {
                            exception.addSuppressed(e);
                        }
                    });
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Remote exception occurred", e.getCause());
        }
    }

    static void multiThreadPersonQueries(final int countOfThreads, final int requestsPerItem, final int countOfPersons,
                                         final int countOfAccounts) throws InterruptedException, RemoteException {
        final MultiThreadPersonData data = new MultiThreadPersonData(countOfPersons, countOfAccounts);
        safeCreateMultiplePersonsWithMultipleAccounts(data.passports, data.subIds);

        multiThreadBase(countOfThreads, requestsPerItem, countOfPersons, countOfAccounts, (i, j) -> {
            try {
                final Account account = safeGetLinkedAccount(data.passports.get(i), data.subIds.get(j));
                account.addAmount(data.deltas.get(i).get(j));
            } catch (final RemoteException e) {
                throw new UncheckedIOException(e);
            }
        });

        validatePersonAccountAmounts(countOfPersons, countOfAccounts, data.passports, data.subIds,
                (i, j) -> data.deltas.get(i).get(j) * requestsPerItem);
    }
}
