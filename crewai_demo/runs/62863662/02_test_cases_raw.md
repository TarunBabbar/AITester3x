[
  {
    "id": "TC001",
    "title": "Successful login with standard_user",
    "priority": "P0",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'standard_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is redirected to the inventory page (https://saucedemo.com/inventory.html) and sees the product list"
  },
  {
    "id": "TC002",
    "title": "Login fails with locked_out_user",
    "priority": "P0",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'locked_out_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message 'Epic sadface: Sorry, this user has been locked out.' is displayed and user remains on login page"
  },
  {
    "id": "TC003",
    "title": "Login fails with invalid password",
    "priority": "P1",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'standard_user' in the username field",
      "Enter 'wrong_password' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message 'Epic sadface: Username and password do not match any user in this service' is displayed"
  },
  {
    "id": "TC004",
    "title": "Login fails with empty username",
    "priority": "P1",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Leave username field empty",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "Error message 'Epic sadface: Username is required' is displayed"
  },
  {
    "id": "TC005",
    "title": "Login fails with empty password",
    "priority": "P1",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'standard_user' in the username field",
      "Leave password field empty",
      "Click the Login button"
    ],
    "expected": "Error message 'Epic sadface: Password is required' is displayed"
  },
  {
    "id": "TC006",
    "title": "Login fails with both fields empty",
    "priority": "P2",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Leave both username and password fields empty",
      "Click the Login button"
    ],
    "expected": "Error message 'Epic sadface: Username is required' is displayed"
  },
  {
    "id": "TC007",
    "title": "Login works with problem_user",
    "priority": "P2",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'problem_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is redirected to inventory page (may have UI issues but login succeeds)"
  },
  {
    "id": "TC008",
    "title": "Login works with performance_glitch_user",
    "priority": "P2",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'performance_glitch_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is redirected to inventory page after noticeable delay (performance glitch)"
  },
  {
    "id": "TC009",
    "title": "Login works with error_user",
    "priority": "P2",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'error_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is redirected to inventory page (may have error states but login succeeds)"
  },
  {
    "id": "TC010",
    "title": "Login works with visual_user",
    "priority": "P2",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter 'visual_user' in the username field",
      "Enter 'secret_sauce' in the password field",
      "Click the Login button"
    ],
    "expected": "User is redirected to inventory page (may have visual differences but login succeeds)"
  },
  {
    "id": "TC011",
    "title": "Username field accepts special characters and long input",
    "priority": "P3",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Enter a 100-character string with special characters in username field",
      "Enter 'secret_sauce' in password field",
      "Click the Login button"
    ],
    "expected": "Appropriate error message displayed (invalid credentials) without application crash"
  },
  {
    "id": "TC012",
    "title": "Accessibility - Tab navigation and focus order",
    "priority": "P1",
    "preconditions": "User is on the login page at https://saucedemo.com",
    "steps": [
      "Press Tab key to navigate from username field",
      "Verify focus moves to password field",
      "Press Tab key again",
      "Verify focus moves to Login button",
      "Press Enter on Login button with valid credentials"
    ],
    "expected": "Focus order follows logical sequence: username -> password -> login button; Enter key submits form successfully"
  }
]