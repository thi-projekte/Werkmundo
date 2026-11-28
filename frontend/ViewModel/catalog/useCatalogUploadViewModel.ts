import { useEffect, useRef, useState } from "react";
import { getCatalogStatus, uploadCatalog } from "../../infrastructure/api/catalogApi";

export function useCatalogUploadViewModel() {
  const statusVersion = useRef(0);
  const [file, setFile] = useState<File | null>(null);
  const [articleCount, setArticleCount] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void getCatalogStatus().then((status) => {
      if (active && statusVersion.current === 0) setArticleCount(status.articleCount);
    }).catch(() => {
      if (active && statusVersion.current === 0) setError("Der Katalogstatus konnte nicht geladen werden. Ein Upload ist trotzdem möglich.");
    });
    return () => { active = false; };
  }, []);

  const chooseFile = (next: File | null) => {
    setFile(next);
    setNotice("");
    setError("");
  };
  const upload = async () => {
    if (!file || busy) return;
    setError("");
    setNotice("");
    if (!file.name.toLowerCase().endsWith(".csv")) {
      setError("Bitte wähle eine CSV-Datei.");
      return;
    }
    if (file.size >= 10 * 1024 * 1024) {
      setError("Bitte wähle eine CSV-Datei unter 10 MB.");
      return;
    }
    statusVersion.current++;
    setBusy(true);
    try {
      const count = await uploadCatalog(file);
      setArticleCount(count);
      setNotice(`Katalog erfolgreich importiert: ${count.toLocaleString("de-DE")} Artikel.`);
    } catch (failure: unknown) {
      setError(failure instanceof Error ? failure.message : "Der Katalog konnte nicht importiert werden.");
    } finally {
      setBusy(false);
    }
  };
  return { file, articleCount, busy, notice, error, chooseFile, upload };
}
