export function Field({
  label,
  type,
  value,
  onChange,
  placeholder,
  required = true,
  disabled = false,
}: {
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-400">
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        className="w-full rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3 text-sm text-white placeholder:text-slate-500 outline-none transition-all focus:border-accent/60 focus:bg-white/[0.06] focus:shadow-glow disabled:cursor-not-allowed disabled:opacity-50"
      />
    </label>
  );
}
