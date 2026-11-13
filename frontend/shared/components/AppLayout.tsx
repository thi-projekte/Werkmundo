import { Outlet } from "react-router-dom";
import { Navbar } from "../../View/shared/navigation/Navbar";

export function AppLayout() {
  return (
    <div className="shell wm-app-shell">
      <main className="shell-content wm-app-content">
        <Outlet />
      </main>
      <Navbar />
    </div>
  );
}