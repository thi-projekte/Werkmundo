import { Outlet } from "react-router-dom";
import { Navbar } from "../../View/shared/navigation/Navbar";
import { SetupProgress } from "../../View/shared/SetupProgress";

export function AppLayout() {
  return (
    <div className="shell wm-app-shell">
      <main className="shell-content wm-app-content">
        <SetupProgress />
        <Outlet />
      </main>
      <Navbar />
    </div>
  );
}