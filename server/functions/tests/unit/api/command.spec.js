import supertest from 'supertest';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { getServer } from '../../../api/index.js';
import { jest } from '@jest/globals';
import { errors } from '../../../middleware/error-handler.js';

let logger = logger = { error: jest.fn(), warn: jest.fn(), log: jest.fn() };
let db;
beforeAll(async () => {
    const testEnv = await initializeTestEnvironment({
        projectId: "rms-one",
        database: { host: '127.0.0.1', port: 9000 },
        storage: { host: '127.0.0.1', port: 9199 },
    });
    const context = testEnv.authenticatedContext('owner');
    db = context.database();
});

describe('GET /command', () => {
    it("should send error response when clientId is not provided", async () => {
        await supertest(getServer({ logger })).get("/command")
            .expect(400)
            .then(res => {
                expect(res.body.status).toEqual(400);
                expect(res.body.code).toEqual(errors.CLIENT_ID_NOT_PROVIDED);
            });
    });
    it("should send error response when clientId doesn't exist", async () => {
        const mockDB = {
            ref: () => ({ get: () => ({ exists: jest.fn(() => false) }) })
        };

        await supertest(getServer({ db: mockDB, logger })).get("/command?id=test1234")
            .expect(404)
            .then(res => {
                expect(res.body.status).toEqual(404);
                expect(res.body.code).toEqual(errors.CLIENT_ID_NOT_FOUND);
            });
    });
    it("should send correct response when commands doesn't exist", async () => {
        const mockDB = {
            ref: () => ({
                get: () => ({ exists: jest.fn(() => true) }),
                child: () => ({
                    get: () => ({ exists: jest.fn(() => false) })
                })
            })
        };

        await supertest(getServer({ db: mockDB, logger })).get("/command?id=test1234")
            .expect(200)
            .then(res => {
                expect(res.body).toEqual([]);
            });
    });
    it("should fetch commands and clear from database", async () => {
        const mockSet = jest.fn();
        const mockDB = {
            ref: () => ({
                get: () => ({ exists: jest.fn(() => true) }),
                child: () => ({
                    get: () => ({
                        exists: jest.fn(() => true),
                        val: jest.fn(() => (['testcmd1', 'testcmd2', 'testcmd3']))
                    }),
                    set: mockSet
                })
            })
        };

        await supertest(getServer({ db: mockDB, logger })).get("/command?id=test1234")
            .expect(200)
            .then(res => {
                expect(res.body).toEqual(['testcmd1', 'testcmd2', 'testcmd3']);
                expect(mockSet).toBeCalledWith([]);
            });
    });
});

describe('POST /command', () => {
    it("should send error response when clientId is not provided", async () => {
        await supertest(getServer({ logger })).post("/command")
            .send(['testcmd1', 'testcmd2'])
            .expect(400)
            .then(res => {
                expect(res.body.status).toEqual(400);
                expect(res.body.code).toEqual(errors.CLIENT_ID_NOT_PROVIDED);
            });
    });
    it("should send error response when commands format is incorrect", async () => {
        await supertest(getServer({ logger })).post("/command?id=test123")
            .send({ notAnArray: 1234 })
            .expect(400)
            .then(res => {
                expect(res.body.status).toEqual(400);
                expect(res.body.code).toEqual(errors.BAD_COMMANDS_FORMAT);
            });
    });
    it("should send error response when clientId doesn't exist", async () => {
        const mockDB = {
            ref: () => ({ get: () => ({ exists: jest.fn(() => false) }) })
        };

        await supertest(getServer({ db: mockDB, logger })).post("/command?id=test123")
            .send(['testcmd1', 'testcmd2'])
            .expect(404)
            .then(res => {
                expect(res.body.status).toEqual(404);
                expect(res.body.code).toEqual(errors.CLIENT_ID_NOT_FOUND);
            });
    });
    it("should succeed when there are no existing commands", async () => {
        const mockSet = jest.fn();
        const mockDB = {
            ref: () => ({
                get: () => ({ exists: jest.fn(() => true) }),
                child: () => ({
                    get: () => ({ exists: jest.fn(() => false) }),
                    set: mockSet
                })
            })
        };

        await supertest(getServer({ db: mockDB, logger })).post("/command?id=test123")
            .send(['testcmd1', 'testcmd2'])
            .expect(200)
            .then(res => {
                expect(res.status).toEqual(200);
                expect(mockSet).toBeCalledWith(['testcmd1', 'testcmd2']);
            });
    });
    it("should succeed when there are existing commands in database", async () => {
        const mockSet = jest.fn();
        const mockDB = {
            ref: () => ({
                get: () => ({ exists: jest.fn(() => true) }),
                child: () => ({
                    get: () => ({
                        exists: jest.fn(() => true),
                        val: jest.fn(() => (['testexist1', 'testexist2']))
                    }),
                    set: mockSet
                })
            })
        };

        await supertest(getServer({ db: mockDB, logger })).post("/command?id=test123")
            .send(['testcmd1', 'testcmd2'])
            .expect(200)
            .then(res => {
                expect(res.status).toEqual(200);
                expect(mockSet).toBeCalledWith(['testexist1', 'testexist2', 'testcmd1', 'testcmd2']);
            });
    });
});
