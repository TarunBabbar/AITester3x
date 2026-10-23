function delay(ms, value) {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

function fetchUser(id) {
  return delay(10, { id, name: `User${id}` });
}

async function loadUsers(ids) {
  const users = await Promise.all(ids.map((id) => fetchUser(id)));
  return users.map((user) => user.name);
}

function fail(shouldFail) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (shouldFail) {
        reject(new Error("Something went wrong"));
      } else {
        resolve("success");
      }
    }, 10);
  });
}

async function main() {
  const value = await delay(10, "resolved value");
  console.log("awaited:", value);

  const names = await loadUsers([1, 2, 3]);
  console.log("loaded users:", names.join(", "));

  const fastest = await Promise.race([delay(5, "fast"), delay(50, "slow")]);
  console.log("race winner:", fastest);

  try {
    await fail(true);
  } catch (error) {
    console.log("caught:", error.message);
  }

  const result = await fail(false).catch(() => "fallback");
  console.log("fallback result:", result);

  const settled = await Promise.allSettled([fail(false), fail(true)]);
  console.log(
    "allSettled:",
    settled.map((entry) => entry.status).join(", ")
  );
}

main().then(() => console.log("done"));
