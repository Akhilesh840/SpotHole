# Test mermaid syntax
code = """flowchart LR
    %% Actors Left
    WebCust["Web Customer"]
    RegCust["Registered Customer"]
    NewCust["New Customer"]

    %% Generalization (pointing to Web Customer)
    RegCust --> WebCust
    NewCust --> WebCust

    %% Subsystem
    subgraph Subsystem["&lt;&lt;Subsystem&gt;&gt; Online Shopping"]
        direction TB
        ViewItems(["View Items"])
        MakePurchase(["Make Purchase"])
        Checkout(["Checkout"])
        ClientRegister(["Client Register"])

        MakePurchase -. "&lt;&lt;include&gt;&gt;" .-> ViewItems
        MakePurchase -. "&lt;&lt;include&gt;&gt;" .-> Checkout
    end

    %% Associations Left
    RegCust --- ViewItems
    RegCust --- MakePurchase
    NewCust --- ViewItems
    NewCust --- ClientRegister

    %% Actors Right
    Auth["&lt;&lt;Service&gt;&gt;<br/>Authentication"]
    IdP["Identity Provider"]
    CreditPay["Credit Payment Service"]
    PayPal["PayPal"]

    %% Associations Right
    ViewItems --- Auth
    MakePurchase --- Auth
    Checkout --- Auth
    ViewItems --- IdP
    Checkout --- IdP
    Checkout --- CreditPay
    Checkout --- PayPal
"""

with open('test_shopping.mmd', 'w', encoding='utf-8') as f:
    f.write(code)

print("Saved test_shopping.mmd")
