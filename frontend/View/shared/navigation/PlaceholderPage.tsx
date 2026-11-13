import { useEffect } from "react";
import type { LucideIcon } from "lucide-react";

type PlaceholderPageProps = {
  title: string;
  accent?: string;
  description: string;
  icon: LucideIcon;
};

/** Einheitliche Seite für Bereiche, deren Inhalt noch umgesetzt wird. */
export function PlaceholderPage({ title, accent, description, icon: Icon }: PlaceholderPageProps) {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${accent ?? title} | Werkmundo`;
    return () => {
      document.title = previousTitle;
    };
  }, [title, accent]);

  return (
    <div className="wm-page">
      <h1>
        {title} {accent && <span>{accent}</span>}
      </h1>
      <section className="wm-placeholder">
        <div className="wm-placeholder-icon">
          <Icon size={22} aria-hidden="true" />
        </div>
        <div>
          <h2>Dieser Bereich ist in Arbeit</h2>
          <p>{description}</p>
        </div>
      </section>
    </div>
  );
}
