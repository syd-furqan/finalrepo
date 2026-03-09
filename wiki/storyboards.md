## 1. Initial Entry: Authentication
The system begins with a unified entry point to ensure secure access.

### **Screen: Login Portal**
* **User Action:** User enters credentials and taps **"Login"**.
* **System Logic:** Based on the user profile, the system redirects to either the **Guard Dashboard** or the **Faculty/Student Dashboard**.  

---

## 2. Branch A: Security Guard & Admin Workflow
*Focus: Real-time verification and gate management.*

### **Step A1: Operations Dashboard**
* **Context:** Guard monitors active requests and daily visitor counts (**US-01**).
* **User Action:** A visitor arrives. Guard taps **"Scan Visitor QR Code"**.
* **Requirement Coverage:** **US-17** (QR Scanning).
    
* <img width="196" height="443.5" alt="image" src="https://github.com/user-attachments/assets/4f882063-c54f-4ffc-ae62-e0bafdeaa3e3" />

### **Step A1.5: Manual Visitor Lookup**
* **Context:** Used when a visitor does not have a QR code or the scan fails.
* **User Action:** Guard enters the visitor's Name, CNIC, or Vehicle Number into the search bar.
* **System Logic:** The system filters the "Expected Visitors" list in real-time.
* **Requirement Coverage:** **US-02** (Manual Search & Lookup).
    
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/7a03c45e-f7dd-4a09-b70a-c498e9fafd44" /> 

### **Step A2: Credential Verification**
* **Context:** The system fetches the visitor's live profile from the university database.
* **User Action:** Guard reviews CNIC, Vehicle Number, and Host details (**US-03**).
* **Decision:**
    * **Grant Entry:** Transitions to success state and logs the event (**US-04**).
    * **Deny Entry:** Guard selects "Deny" and adds a reason for the log (**US-05**).
* **Requirement Coverage:** **US-03, US-04, US-05**.
* **Exception Handling (US-18):** If a scanned QR is expired or blacklisted, the system transitions to an **Alert State** (Red Header), notifying the Guard and logging a "Security Incident" for the Admin's audit log.
    
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/37ac12cb-3cf7-492d-9f87-59a1d902dcf5" />

### **Step A3: Security Audit & Reporting**
* **Context:** Admin or Guard views a scrollable, immutable history of all movements (**US-13**).
* **User Action:** While Guards have "View Only" access, the **Admin** persona can tap the **"Download"** icon to generate PDF/CSV reports (**US-15**).
* **Requirement Coverage:** **US-13, US-15**.
  
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/0fea30d1-0f76-4feb-bc75-d9fff6ca4b16" />

---

## 3. Branch B: Faculty & Student Workflow
*Focus: Request creation and guest pass management.*

### **Step B1: User Dashboard**
* **Context:** User views current status of guest invitations (**US-09**).
* **User Action:** User taps the **"+"** Floating Action Button to initiate a new request.
* **Requirement Coverage:** **US-09**.  

* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/21a4caa6-e95b-41e2-826c-51e30e609e18" />


### **Step B2: Guest Pass Creation**
* **Context:** A 3-step wizard to input visitor data for the **Final Release**.
* **User Action:** User enters guest details, vehicle info, and sets a custom **Expiry Time** (**US-11**).
* **Requirement Coverage:** **US-06, US-08, US-10, US-11**.  
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/eed431e4-2790-4ac9-8e3b-436a3bfe8044" />

### **Step B3: Distribution & Revocation**
* **Context:** Integrated dashboard showing the generated pass with a "Green/Approved" status.
* **User Action:** * User taps **"Share QR"** to send the pass to the guest (**US-07**).
    * User taps **"Cancel"** to revoke access if plans change (**US-12**).
* **Requirement Coverage:** **US-07, US-12**.  
* <img width="196" height="443" alt="image" src="https://github.com/user-attachments/assets/1ad8df6d-02de-4879-b590-fdef78364c2e" />





