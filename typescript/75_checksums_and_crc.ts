const CRC_TABLE: Uint32Array = (() => {
  const table = new Uint32Array(256);
  for (let index = 0; index < 256; index += 1) {
    let value = index;
    for (let bit = 0; bit < 8; bit += 1) {
      value = (value & 1) === 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
    }
    table[index] = value >>> 0;
  }
  return table;
})();

function utf8(text: string): Uint8Array {
  return new TextEncoder().encode(text);
}

function crc32(bytes: Uint8Array): number {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc = (CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8)) >>> 0;
  }
  return (crc ^ 0xffffffff) >>> 0;
}

/** Incremental CRC-32: feed chunks with update() and read digest() at the end. */
class Crc32 {
  private state = 0xffffffff;

  update(bytes: Uint8Array): this {
    let crc = this.state;
    for (const byte of bytes) {
      crc = (CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8)) >>> 0;
    }
    this.state = crc;
    return this;
  }

  digest(): number {
    return (this.state ^ 0xffffffff) >>> 0;
  }
}

function adler32(bytes: Uint8Array): number {
  const MODULUS = 65521;
  let low = 1;
  let high = 0;

  for (const byte of bytes) {
    low = (low + byte) % MODULUS;
    high = (high + low) % MODULUS;
  }

  return ((high << 16) | low) >>> 0;
}

class Adler32 {
  private low = 1;
  private high = 0;

  update(bytes: Uint8Array): this {
    const MODULUS = 65521;
    for (const byte of bytes) {
      this.low = (this.low + byte) % MODULUS;
      this.high = (this.high + this.low) % MODULUS;
    }
    return this;
  }

  digest(): number {
    return ((this.high << 16) | this.low) >>> 0;
  }
}

function fnv1a(bytes: Uint8Array): number {
  let hash = 0x811c9dc5;
  for (const byte of bytes) {
    hash ^= byte;
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash >>> 0;
}

function toHex(value: number): string {
  return (value >>> 0).toString(16).padStart(8, "0");
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- known vectors ---");
  const vectors: Array<[string, number | null, number | null, number | null]> = [
    ["", 0x00000000, 0x00000001, 0x811c9dc5],
    ["a", 0xe8b7be43, 0x00620062, 0xe40c292c],
    ["123456789", 0xcbf43926, 0x091e01de, null],
    ["Wikipedia", null, 0x11e60398, null],
  ];

  for (const [text, crc, adler, fnv] of vectors) {
    const bytes = utf8(text);
    const actualCrc = crc32(bytes);
    const actualAdler = adler32(bytes);
    const actualFnv = fnv1a(bytes);
    console.log(
      `${JSON.stringify(text).padEnd(13)} crc32 ${toHex(actualCrc)} adler32 ${toHex(actualAdler)} fnv1a ${toHex(actualFnv)}`,
    );
    if (crc !== null) {
      check(actualCrc === crc, `crc32 mismatch for ${JSON.stringify(text)}`);
    }
    if (adler !== null) {
      check(actualAdler === adler, `adler32 mismatch for ${JSON.stringify(text)}`);
    }
    if (fnv !== null) {
      check(actualFnv === fnv, `fnv1a mismatch for ${JSON.stringify(text)}`);
    }
  }

  console.log("\n--- hex formatting ---");
  check(toHex(0) === "00000000", "zero is padded");
  check(toHex(0xff) === "000000ff", "small values are padded");
  check(toHex(0xdeadbeef) === "deadbeef", "all four bytes are printed");
  console.log("00000000 000000ff deadbeef");

  console.log("\n--- incremental crc32 matches one-shot ---");
  const message = "The quick brown fox jumps over the lazy dog";
  const bytes = utf8(message);
  const oneShot = crc32(bytes);
  for (const chunkSize of [1, 2, 7, 13, 41, message.length]) {
    const incremental = new Crc32();
    for (let start = 0; start < bytes.length; start += chunkSize) {
      incremental.update(bytes.subarray(start, start + chunkSize));
    }
    check(incremental.digest() === oneShot, `chunk size ${chunkSize} disagreed`);
  }
  console.log(`chunk sizes 1, 2, 7, 13, 41, ${message.length} all produced ${toHex(oneShot)}`);

  console.log("\n--- incremental adler32 matches one-shot ---");
  const adlerOneShot = adler32(bytes);
  for (const chunkSize of [1, 3, 11, message.length]) {
    const incremental = new Adler32();
    for (let start = 0; start < bytes.length; start += chunkSize) {
      incremental.update(bytes.subarray(start, start + chunkSize));
    }
    check(incremental.digest() === adlerOneShot, `adler chunk size ${chunkSize} disagreed`);
  }
  console.log("incremental adler32 agrees with the one-shot value", toHex(adlerOneShot));

  console.log("\n--- empty chunks are harmless ---");
  const emptyChunks = new Crc32().update(new Uint8Array(0)).update(new Uint8Array(0));
  check(emptyChunks.digest() === 0, "an empty stream has crc 0");
  check(new Adler32().update(new Uint8Array(0)).digest() === 1, "an empty stream has adler 1");

  console.log("\n--- a single flipped bit changes the checksum ---");
  const sample = utf8("integrity matters");
  const baseline = crc32(sample);
  let distinct = 0;
  for (let byteIndex = 0; byteIndex < sample.length; byteIndex += 1) {
    for (const mask of [0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80]) {
      const mutated = sample.slice();
      mutated[byteIndex] ^= mask;
      if (crc32(mutated) !== baseline) {
        distinct += 1;
      }
    }
  }
  console.log(`${distinct} of ${sample.length * 8} single-bit mutations were detected`);
  check(distinct === sample.length * 8, "every single-bit change must change the checksum");

  console.log("\n--- utf-8 handling ---");
  const unicode = "café naïve 🚀";
  const encoded = utf8(unicode);
  check(encoded.length > unicode.length, "astral characters take more than one byte");
  const unicodeCrc = crc32(encoded);
  check(unicodeCrc === crc32(utf8(unicode)), "the same text always hashes the same");
  check(unicodeCrc !== crc32(utf8(unicode.normalize("NFD"))), "normalisation changes the bytes");
  console.log(`"${unicode}" -> ${encoded.length} bytes, crc32 ${toHex(unicodeCrc)}`);

  console.log("\n--- fnv-1a is cheap and deterministic ---");
  const many = Array.from({ length: 1_000 }, (_, index) => fnv1a(utf8(`key-${index}`)));
  const unique = new Set(many);
  console.log(`1000 keys produced ${unique.size} distinct 32-bit hashes`);
  check(unique.size > 990, "collisions should be rare at this size");
  check(many.every((value) => value >= 0 && value <= 0xffffffff), "hashes stay in 32 bits");

  console.log("\nAll checks passed.");
}

main();
