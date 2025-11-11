/**
 * Configuration globale pour les tests unitaires
 * Ce fichier est chargé avant tous les tests
 */

// Mock global pour les objets du navigateur
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // deprecated
    removeListener: jest.fn(), // deprecated
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock pour localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

// Mock pour sessionStorage
const sessionStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
Object.defineProperty(window, 'sessionStorage', {
  value: sessionStorageMock,
});

// Mock pour window.confirm
Object.defineProperty(window, 'confirm', {
  writable: true,
  value: jest.fn(),
});

// Mock pour window.alert
Object.defineProperty(window, 'alert', {
  writable: true,
  value: jest.fn(),
});

// Suppression des logs de console pendant les tests
const originalConsoleError = console.error;
const originalConsoleWarn = console.warn;

beforeAll(() => {
  console.error = (...args: any[]) => {
    if (typeof args[0] === 'string' && (args[0].includes('Warning:') || args[0].includes('Error:'))) {
      return;
    }
    originalConsoleError.apply(console, args);
  };

  console.warn = (...args: any[]) => {
    if (typeof args[0] === 'string' && args[0].includes('Warning:')) {
      return;
    }
    originalConsoleWarn.apply(console, args);
  };
});

afterAll(() => {
  console.error = originalConsoleError;
  console.warn = originalConsoleWarn;
});

// Configuration globale pour les tests asynchrones
jest.setTimeout(10000);

// Mock pour les timers
jest.useFakeTimers();

// Configuration pour les tests de formulaires
beforeEach(() => {
  // Reset des mocks avant chaque test
  jest.clearAllMocks();
  localStorageMock.clear();
  sessionStorageMock.clear();
});
