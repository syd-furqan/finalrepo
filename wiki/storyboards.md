## 1. Initial Entry: Authentication
The system begins with a unified entry point to ensure secure access.

### **Screen: Login Portal**
* **User Action:** User enters credentials and taps **"Login"**.
* **State Transition:** `Logged_Out` → `Authenticating` → `Dashboard_Redirect`.
* **System Logic:** Based on the user profile, the system redirects to either the **Guard Dashboard** or the **Faculty/Student Dashboard**.  


## 2. Branch A: Security Guard & Admin Workflow
*Focus: Real-time verification and gate management.*

### **Step A1: Operations Dashboard**
* **Context:** Guard monitors active requests and daily visitor counts (**US-01**).
* **User Action:** A visitor arrives. Guard taps **"Scan Visitor QR Code"**.
* **State Transition:** `Dashboard_Idle` → `Scanner_Active`.
* **Requirement Coverage:** **US-17** (QR Scanning).
    
* <img width="196" height="443.5" alt="image" src="https://github.com/user-attachments/assets/4f882063-c54f-4ffc-ae62-e0bafdeaa3e3" />

### **Step A1.5: Manual Visitor Lookup**
* **Context:** Used when a visitor does not have a QR code or the scan fails.
* **User Action:** Guard enters the visitor's Name, CNIC, or Vehicle Number into the search bar.
* **System Logic:** The system filters the "Expected Visitors" list in real-time.
* **Transition:** `Dashboard_Idle` → `Lookup_View` → `Verify_Credential`.
* **Requirement Coverage:** **US-02** (Manual Search & Lookup)
    
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/7a03c45e-f7dd-4a09-b70a-c498e9fafd44" /> 

### **Step A2: Credential Verification**
* **Context:** The system fetches the visitor's live profile from the university database.
* **User Action:** Guard reviews CNIC, Vehicle Number, and Host details (**US-03**).
* **Decision:**
    * **Grant Entry:** Transitions to success state and logs the event (**US-04**).
    * **Deny Entry:** Guard selects "Deny" and adds a reason for the log (**US-05**).
* **Requirement Coverage:** **US-03, US-05**.
    
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/37ac12cb-3cf7-492d-9f87-59a1d902dcf5" />

### **Step A3: Security Audit & Reporting**
* **Context:** Admin or Guard views a scrollable, immutable history of all movements (**US-13**).
* **User Action:** Admin taps the **"Download"** icon.
* **Transition:** `View_Logs` → `Export_Report`.
* **Requirement Coverage:** **US-13, US-15**.
  
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/0fea30d1-0f76-4feb-bc75-d9fff6ca4b16" />

## 3. Branch B: Faculty & Student Workflow
*Focus: Request creation and guest pass management.*

### **Step B1: User Dashboard**
* **Context:** User views current status of guest invitations (**US-09**).
* **User Action:** User taps the **"+"** Floating Action Button.
* **State Transition:** `Dashboard_Idle` → `Request_Form_Entry`.

* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/21a4caa6-e95b-41e2-826c-51e30e609e18" />


### **Step B2: Guest Pass Creation**
* **Context:** A 3-step wizard to input visitor data.
* **User Action:** User enters guest details, vehicle info, and sets an expiry time (**US-06, US-08, US-11**).
* **Requirement Coverage:** **US-06, US-08, US-10, US-11**.  
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/eed431e4-2790-4ac9-8e3b-436a3bfe8044" />





