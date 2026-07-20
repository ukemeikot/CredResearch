"use client";

import { use, useEffect, useState } from "react";
import { CheckCircle2, Loader2 } from "lucide-react";
import { api, ApiError, type SurveyQuestion, type SurveyView } from "@/lib/api";
import { Button } from "@/components/ui/button";

/** Public survey surface — account-less respondents answer via the tokenized link (Phase 7, FR-Q). */
export default function SurveyPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = use(params);
  const [survey, setSurvey] = useState<SurveyView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [answers, setAnswers] = useState<Record<string, unknown>>({});
  const [consent, setConsent] = useState(false);
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    api
      .surveyRender(token)
      .then(setSurvey)
      .catch((e) => setError(e instanceof ApiError ? e.message : "This survey is unavailable."))
      .finally(() => setLoading(false));
  }, [token]);

  function set(qid: string, value: unknown) {
    setAnswers((a) => ({ ...a, [qid]: value }));
  }

  async function submit() {
    if (!survey) return;
    for (const q of survey.questions) {
      if (q.required && (answers[q.id] == null || answers[q.id] === "" || (Array.isArray(answers[q.id]) && (answers[q.id] as unknown[]).length === 0))) {
        setError(`Please answer: ${q.prompt}`);
        return;
      }
    }
    if (survey.consentText && !consent) {
      setError("Please give consent to submit.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await api.surveySubmit(token, {
        consentGiven: consent,
        answers: survey.questions
          .filter((q) => answers[q.id] != null && answers[q.id] !== "")
          .map((q) => ({ questionId: q.id, value: answers[q.id] })),
      });
      setDone(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn’t submit your response.");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="grid min-h-screen place-items-center bg-white"><Loader2 className="h-8 w-8 animate-spin text-accent" /></div>;
  if (error && !survey) {
    return (
      <div className="grid min-h-screen place-items-center bg-white px-6 text-center">
        <div><p className="text-lg font-semibold text-slate-900">Survey unavailable</p><p className="mt-2 text-sm text-slate-500">{error}</p></div>
      </div>
    );
  }
  if (done) {
    return (
      <div className="grid min-h-screen place-items-center bg-white px-6 text-center">
        <div>
          <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-600" />
          <p className="mt-3 text-lg font-semibold text-slate-900">Thank you!</p>
          <p className="mt-1 text-sm text-slate-500">Your response has been recorded.</p>
        </div>
      </div>
    );
  }
  if (!survey) return null;

  return (
    <div className="min-h-screen bg-white px-4 py-10 text-slate-700">
      <div className="mx-auto max-w-2xl">
        <h1 className="font-display text-2xl font-bold text-slate-900">{survey.title}</h1>
        {survey.consentText && (
          <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
            <p>{survey.consentText}</p>
            <label className="mt-3 flex items-center gap-2 text-sm">
              <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} /> I consent to participate
            </label>
          </div>
        )}

        <div className="mt-6 space-y-5">
          {survey.questions.map((q, i) => (
            <div key={q.id} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-900">
                {i + 1}. {q.prompt} {q.required && <span className="text-rose-600">*</span>}
              </p>
              <div className="mt-2">
                <Field q={q} value={answers[q.id]} onChange={(v) => set(q.id, v)} />
              </div>
            </div>
          ))}
        </div>

        {error && <p className="mt-4 text-sm text-rose-600">{error}</p>}
        <Button size="lg" className="mt-6 w-full" onClick={submit} disabled={submitting}>
          {submitting ? "Submitting…" : "Submit response"}
        </Button>
      </div>
    </div>
  );
}

function Field({ q, value, onChange }: { q: SurveyQuestion; value: unknown; onChange: (v: unknown) => void }) {
  const choices: string[] = Array.isArray(q.options?.choices) ? q.options.choices : [];
  const input = "w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none";
  switch (q.type) {
    case "LONG_TEXT":
      return <textarea rows={3} className={input} value={(value as string) ?? ""} onChange={(e) => onChange(e.target.value)} />;
    case "NUMBER":
      return <input type="number" className={input} value={(value as string) ?? ""} onChange={(e) => onChange(e.target.value)} />;
    case "BOOLEAN":
      return (
        <div className="flex gap-2">
          {["Yes", "No"].map((o) => (
            <button key={o} onClick={() => onChange(o)} className={`rounded-lg border px-4 py-1.5 text-sm ${value === o ? "border-accent/60 bg-accent/10 text-slate-900" : "border-slate-200 text-slate-500"}`}>{o}</button>
          ))}
        </div>
      );
    case "SINGLE_CHOICE":
    case "LIKERT":
      return (
        <div className="flex flex-wrap gap-2">
          {choices.map((o) => (
            <button key={o} onClick={() => onChange(o)} className={`rounded-lg border px-3 py-1.5 text-sm ${value === o ? "border-accent/60 bg-accent/10 text-slate-900" : "border-slate-200 text-slate-500 hover:border-slate-300"}`}>{o}</button>
          ))}
        </div>
      );
    case "MULTI_CHOICE": {
      const arr = Array.isArray(value) ? (value as string[]) : [];
      return (
        <div className="flex flex-wrap gap-2">
          {choices.map((o) => {
            const on = arr.includes(o);
            return (
              <button
                key={o}
                onClick={() => onChange(on ? arr.filter((x) => x !== o) : [...arr, o])}
                className={`rounded-lg border px-3 py-1.5 text-sm ${on ? "border-accent/60 bg-accent/10 text-slate-900" : "border-slate-200 text-slate-500 hover:border-slate-300"}`}
              >
                {o}
              </button>
            );
          })}
        </div>
      );
    }
    default:
      return <input className={input} value={(value as string) ?? ""} onChange={(e) => onChange(e.target.value)} />;
  }
}
