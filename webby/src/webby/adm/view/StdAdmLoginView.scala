package webby.adm.view
import webby.adm.AdmTrait
import webby.commons.text.html.StdHtmlView

class StdAdmLoginView(adm: AdmTrait, errorMessage: Option[String], login: Option[String])
  extends StdHtmlView {

  DOCTYPE_html

  htmlTag {
    headTag {
      metaCharsetUtf8
      title(adm.loginTitle)
      metaViewport("width=device-width, initial-scale=1.0")
      metaRobots("noindex, nofollow")

      style("""
* {
  box-sizing: border-box;
}
body {
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 14px;
  color: #333333;
  padding: 0;
  margin: 40px auto;
  background: #f5f5f5;
}

form {
  max-width: 300px;
  padding: 19px 29px 29px;
  margin: 0 auto 20px;
  background-color: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 5px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

h1 {
  font-size: 30px;
  font-weight: 500;
  margin: 20px 0 10px;
}

input[type="text"],
input[type="password"] {
  font-size: 16px;
  line-height: 1.428571429;
  height: auto;
  margin-bottom: 15px;
  padding: 7px 9px;
  display: block;
  width: 100%;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
  transition: border-color ease-in-out 0.15s, box-shadow ease-in-out 0.15s;
}

input[type="text"]:focus,
input[type="password"]:focus {
  border-color: #66afe9;
  outline: none;
  box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px rgba(102, 175, 233, 0.6);
}

.error {
  color: #a94442;
  background-color: #f2dede;
  border: 1px solid #ebccd1;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 15px;
}

button {
  display: block;
  width: 100%;
  padding: 10px 0;
  font-size: 18px;
  line-height: 1.33;
  border-radius: 6px;
  color: #fff;
  background-color: #337ab7;
  border: 1px solid #2e6da4;
  text-align: center;
  cursor: pointer;
  outline: none;
}

button:hover,
button:focus {
  color: #ffffff;
  background-color: #286090;
  border-color: #204d74;
}

button:active {
  background-color: #204d74;
  border-color: #122b40;
  box-shadow: inset 0 3px 5px rgba(0,0,0,.125);
}
""")
    }
    bodyTag {
      form.action(adm.route.login).methodPost {
        h1 ~ adm.loginTitle

        inputText.name("login").placeholder("Login").valueSafe(login)
        inputPassword.name("password").placeholder("Password")

        errorMessage.foreach {msg =>
          div.cls("error") ~ msg
        }

        buttonSubmit.cls("btn btn-lg btn-primary btn-block") ~ "Sign in"
      }
    }
  }
}
