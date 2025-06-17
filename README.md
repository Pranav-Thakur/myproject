# 🧠 Spreadsheet Graph Analyzer

A Spring Boot application that transforms spreadsheets into interactive graph structures using Neo4j. Designed to analyze data flow, formula dependencies, and semantic relationships in Excel or Google Sheets — with live syncing and AI-powered insights.

---

## 🚀 Features

- 📥 **Import Spreadsheets** from Google Sheets or Excel
- 🧩 **Node & Relationship Creation**: Cells, Sheets, Formulas, Functions
- 🔍 **Semantic Labeling** for better analysis (e.g., Revenue, Campaign, Total)
- 📊 **Graph Queries** for dependencies and lineage
- 🔁 **Live Sheet Syncing** with Google APIs (via polling or webhook-like update logic) (as off now call /{id}/analyse api again)
- 🤖 **AI Integration** with Gemini API for formula interpretation (secured)

---

## 🏗️ Tech Stack

| Layer         | Technology                |
|---------------|---------------------------|
| Backend       | Spring Boot (Java)        |
| Database      | Neo4j (Graph DB)          |
| Sheet Access  | Google Sheets API / Apache POI |
| AI Analysis   | Gemini API (via secure key reading) |
| Build Tool    | Maven                     |

---

📡 Endpoints Overview (/api/v1)

Endpoint	Description

/spreadsheets/{id}/analyze	Reads sheet & builds graph

/query	Run custom queries

/cells/{cellId}/impact	shows impact of cellId on other cells depending on it

/graph/visualize	get complete stored graph

/ai/suggest	 Suggest graph improvements using gemini



## 🧰 Setup Instructions

### 1. Clone the Repo

```bash
git clone https://github.com/Pranav-Thakur/myproject
cd myproject
```

In **src/main/resources** put files like
superjoinai-googlesheet-api.json  => api related key
geminin.key => gemini api key

then run
```bash
mvn clean install
mvn spring-boot:run
```

App runs on: http://localhost:8080/index.html



🧠 AI & Formula Analysis
Gemini integration is used to interpret formulas and annotate them.

Use graphQueryService.getGraphSummary() to summarize graph insights.


📈 Future Roadmap

✅ Live sync with delta updates from Google Sheets

🔜 Frontend visualization panel

🔐 OAuth2 integration for user-level sheet access

🤝 GitHub App to sync PR comments with spreadsheet metadata (experimental)




📜 License
MIT License — use freely, contribute enthusiastically!


