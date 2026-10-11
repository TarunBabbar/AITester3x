interface Settings {
  theme: string;
  fontSize: number;
  autoSave: boolean;
}

type Optional<T> = {
  [K in keyof T]?: T[K];
};

type ReadonlyDeep<T> = {
  readonly [K in keyof T]: T[K];
};

type Getters<T> = {
  [K in keyof T as `get${Capitalize<string & K>}`]: () => T[K];
};

type Mutable<T> = {
  -readonly [K in keyof T]: T[K];
};

const partial: Optional<Settings> = { theme: "dark" };
console.log("Optional:", partial);

const settings: Settings = { theme: "light", fontSize: 14, autoSave: true };
const readonlySettings: ReadonlyDeep<Settings> = settings;

// readonlySettings.theme = "dark"; // Error: read-only

const getters: Getters<Settings> = {
  getTheme: () => settings.theme,
  getFontSize: () => settings.fontSize,
  getAutoSave: () => settings.autoSave,
};

console.log("Getters:", getters.getTheme(), getters.getFontSize(), getters.getAutoSave());

const mutable: Mutable<ReadonlyDeep<Settings>> = { ...settings };
mutable.theme = "solarized";
console.log("Mutable after change:", mutable.theme);
