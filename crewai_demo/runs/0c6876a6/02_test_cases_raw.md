[
  {
    "id": "TC001",
    "title": "Valid login with standard_user",
    "priority": "P0",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'standard_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is successfully logged in and redirected to the inventory page (/inventory.html)"
  },
  {
    "id": "TC002",
    "title": "Login with locked_out_user shows error",
    "priority": "P0",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'locked_out_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Sorry, this user has been locked out.' and user remains on login page"
  },
  {
    "id": "TC003",
    "title": "Valid login with problem_user",
    "priority": "P1",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'problem_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is successfully logged in and redirected to the inventory page (may have UI issues per design)"
  },
  {
    "id": "TC004",
    "title": "Valid login with performance_glitch_user",
    "priority": "P1",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'performance_glitch_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is successfully logged in after noticeable delay and redirected to the inventory page"
  },
  {
    "id": "TC005",
    "title": "Valid login with error_user",
    "priority": "P1",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'error_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is successfully logged in and redirected to the inventory page (may trigger error states per design)"
  },
  {
    "id": "TC006",
    "title": "Valid login with visual_user",
    "priority": "P1",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'visual_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is successfully logged in and redirected to the inventory page (may have visual differences per design)"
  },
  {
    "id": "TC007",
    "title": "Invalid username with valid password",
    "priority": "P1",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'invalid_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Username and password do not match any user in this service' and user remains on login page"
  },
  {
    "id": "TC008",
    "title": "Valid username with invalid password",
    "priority": "P1",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'standard_user' in the username field",
      "Enter 'wrong_password' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Username and password do not match any user in this service' and user remains on login page"
  },
  {
    "id": "TC009",
    "title": "Empty username with valid password",
    "priority": "P2",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Leave username field empty",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Username is required' and user remains on login page"
  },
  {
    "id": "TC010",
    "title": "Valid username with empty password",
    "priority": "P2",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'standard_user' in the username field",
      "Leave password field empty",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Password is required' and user remains on login page"
  },
  {
    "id": "TC011",
    "title": "Both username and password empty",
    "priority": "P2",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Leave username field empty",
      "Leave password field empty",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Username is required' and user remains on login page"
  },
  {
    "id": "TC012",
    "title": "Special characters in username and password",
    "priority": "P3",
    "preconditions": "User is on the login page at https://www.saucedemo.com",
    "steps": [
      "Enter 'standard_user@#$%' in the username field",
      "Enter 'secret_sauce!@#' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message displayed: 'Epic sadface: Username and password do not match any user in this service' and user remains on login page"
  }
]