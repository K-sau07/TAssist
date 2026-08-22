import { useRef } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { FileText, MessageCircleQuestion, Quote, FileCheck2, Users, ShieldCheck } from 'lucide-react'
import { HeroAnimation } from './parts/HeroAnimation'
import { Button } from '@/design/components/Button'
import { useAuthStore } from '@/lib/auth/store'

const reveal = {
  initial: { opacity: 0, y: 16 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true, margin: '-80px' },
  transition: { type: 'spring', stiffness: 140, damping: 22 },
}

export default function LandingPage() {
  const loggedIn = useAuthStore((s) => Boolean(s.token))
  const howRef = useRef<HTMLElement>(null)

  return (
    <div className="min-h-screen bg-bg text-text">
      {/* 1. sticky nav */}
      <header className="sticky top-0 z-30 border-b border-border/60 bg-bg/80 backdrop-blur">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <Link to="/" className="font-display text-xl text-primary">TAssist</Link>
          <div className="flex items-center gap-2">
            {loggedIn ? (
              <Link to="/app"><Button>Open app</Button></Link>
            ) : (
              <>
                <Link to="/login"><Button variant="ghost">Log in</Button></Link>
                <Link to="/signup"><Button>Sign up</Button></Link>
              </>
            )}
          </div>
        </nav>
      </header>

      {/* 2. hero */}
      <section className="mx-auto grid max-w-6xl items-center gap-8 px-6 py-16 md:grid-cols-2 md:py-24">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 140, damping: 22 }}>
          <h1 className="font-display text-4xl leading-tight">
            Your files. One brain.<br /><span className="text-primary">Zero uploads to Claude.</span>
          </h1>
          <p className="mt-5 max-w-md text-lg text-text-muted">
            Upload once. Ask anything. Answers grounded in your own documents — never invented, always cited.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/signup"><Button size="lg">Get started free</Button></Link>
            <Button size="lg" variant="secondary"
              onClick={() => howRef.current?.scrollIntoView({ behavior: 'smooth' })}>
              See how it works
            </Button>
          </div>
        </motion.div>
        <div className="mx-auto h-[320px] w-full max-w-sm md:h-[380px]">
          <HeroAnimation />
        </div>
      </section>

      {/* 3. how it works */}
      <section ref={howRef} className="mx-auto max-w-6xl px-6 py-16">
        <motion.h2 {...reveal} className="text-center font-display text-3xl">How it works</motion.h2>
        <div className="mt-10 grid gap-6 md:grid-cols-3">
          {[
            { icon: FileText, title: 'Drop in your files', body: 'PDFs, docs, slides, sheets, notes — all in one library.' },
            { icon: MessageCircleQuestion, title: 'Ask in plain English', body: 'Mention a file with @, or scope a chat to a folder.' },
            { icon: FileCheck2, title: 'Get answers with sources', body: 'Every answer cites the exact document it came from.' },
          ].map((s, i) => (
            <motion.div key={s.title} {...reveal} transition={{ ...reveal.transition, delay: i * 0.08 }}
              className="rounded-lg border border-border bg-bg-elev p-6 shadow-1 transition-transform hover:-translate-y-1 hover:shadow-2">
              <div className="grid h-12 w-12 place-items-center rounded-md bg-bg-sunken text-primary">
                <s.icon size={24} strokeWidth={1.75} />
              </div>
              <h3 className="mt-4 text-lg">{s.title}</h3>
              <p className="mt-1 text-sm text-text-muted">{s.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* 4. channel pitch */}
      <section className="bg-bg-sunken py-16">
        <div className="mx-auto grid max-w-6xl items-center gap-8 px-6 md:grid-cols-2">
          <motion.div {...reveal}>
            <h2 className="font-display text-3xl">Publish a Q&A channel over your knowledge</h2>
            <p className="mt-4 text-text-muted">
              A TA fielding the same questions. A manager onboarding a team. A creator with a paid community.
              Attach a subset of your files, invite people, and let them ask — they get grounded answers
              without ever seeing the underlying documents.
            </p>
          </motion.div>
          <motion.div {...reveal} className="flex items-center justify-center gap-6 rounded-lg border border-border bg-bg-elev p-8 shadow-1">
            <Users size={64} strokeWidth={1.25} className="text-primary" />
          </motion.div>
        </div>
      </section>

      {/* 5. grounding guarantee */}
      <section className="mx-auto max-w-3xl px-6 py-20">
        <motion.div {...reveal} className="rounded-xl border-2 border-primary/20 bg-bg-elev p-10 text-center shadow-1">
          <ShieldCheck size={40} strokeWidth={1.5} className="mx-auto text-primary" />
          <p className="mt-4 font-display text-2xl leading-snug">
            TAssist only answers from your documents. If the answer isn't there, we tell you — we don't guess.
          </p>
          <p className="mt-4 text-sm text-text-muted">
            Under the hood: retrieval-augmented generation. Your question finds the most relevant passages
            from your files, and the model answers only from those — with citations you can open and verify.
          </p>
        </motion.div>
      </section>

      {/* 6. file-type strip */}
      <section className="mx-auto max-w-4xl px-6 pb-16">
        <p className="mb-4 text-center text-sm uppercase tracking-widest text-text-faint">Works with</p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          {['PDF', 'DOCX', 'PPTX', 'XLSX', 'CSV', 'TXT', 'MD'].map((t) => (
            <span key={t} className="inline-flex items-center gap-1.5 rounded-round border border-border bg-bg-elev px-3 py-1.5 text-sm text-text-muted">
              <Quote size={13} strokeWidth={1.75} /> {t}
            </span>
          ))}
        </div>
      </section>

      {/* 7. footer */}
      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-6 py-8 text-sm text-text-faint sm:flex-row">
          <span className="font-display text-lg text-primary">TAssist</span>
          <div className="flex gap-5">
            <span className="hover:text-text-muted">Privacy</span>
            <span className="hover:text-text-muted">Terms</span>
          </div>
          <span>© {new Date().getFullYear()} TAssist</span>
        </div>
      </footer>
    </div>
  )
}
