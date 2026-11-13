import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { initKeycloak } from "./infrastructure/auth/keycloakClient";
import "./shared/styles/werkmundo.css";

const root = document.getElementById("root")!;

void initKeycloak()
  .catch((error: unknown) => {
    console.error("Keycloak konnte nicht initialisiert werden:", error);
  })
  .finally(() => {
    ReactDOM.createRoot(root).render(
      <React.StrictMode>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </React.StrictMode>,
    );
  });
