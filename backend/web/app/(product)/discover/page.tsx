"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type CohortRecommendation, type Offer } from "@/lib/api";
import { useSession } from "@/lib/session";
import {
  Button,
  Card,
  CardTitle,
  EmptyState,
  Notice,
  PageHeading,
  PaywallCard,
  Reveal,
  Skeleton,
  Tag,
  cx,
} from "@/components/ui";
import { DiscoverIcon, SparkIcon } from "@/components/icons";

function formatEffect(value: number): { text: string; improvement: boolean } {
  const improvement = value < 0;
  const sign = value > 0 ? "+" : value < 0 ? "−" : "";
  const text = `${sign}${Math.abs(value).toFixed(2)}`;
  return { text, improvement };
}

function formatPrice(cents: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency,
    }).format(cents / 100);
  } catch {
    return `${(cents / 100).toFixed(2)} ${currency}`;
  }
}

export default function DiscoverPage() {
  const { userId, isPremium, refresh } = useSession();

  const [cohorts, setCohorts] = useState<CohortRecommendation[] | null>(null);
  const [offers, setOffers] = useState<Offer[] | null>(null);
  const [cohortError, setCohortError] = useState<string | null>(null);
  const [offerError, setOfferError] = useState<string | null>(null);

  const [upgrading, setUpgrading] = useState(false);
  const [upgradeError, setUpgradeError] = useState<string | null>(null);

  const [offerLoadingId, setOfferLoadingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!userId || !isPremium) return;
    await Promise.all([
      (async () => {
        setCohortError(null);
        try {
          const res = await api.discover(userId);
          setCohorts(res.recommendations);
        } catch (e) {
          setCohorts([]);
          setCohortError(
            e instanceof Error ? e.message : "Could not load cohort signals",
          );
        }
      })(),
      (async () => {
        setOfferError(null);
        try {
          setOffers(await api.offers(userId));
        } catch (e) {
          setOffers([]);
          setOfferError(
            e instanceof Error ? e.message : "Could not load affiliate offers",
          );
        }
      })(),
    ]);
  }, [userId, isPremium]);

  useEffect(() => {
    setCohorts(null);
    setOffers(null);
    load();
  }, [load]);

  const handleUnlock = useCallback(async () => {
    if (!userId) return;
    setUpgradeError(null);
    setUpgrading(true);
    try {
      await api.upgrade(userId);
      await refresh();
    } catch (e) {
      setUpgradeError(
        e instanceof Error ? e.message : "Could not complete the upgrade",
      );
    } finally {
      setUpgrading(false);
    }
  }, [userId, refresh]);

  const handleOfferClick = useCallback(
    async (offerId: string) => {
      if (!userId) return;
      setOfferLoadingId(offerId);
      setOfferError(null);
      try {
        const { url } = await api.clickOffer(userId, offerId);
        window.open(url, "_blank", "noopener,noreferrer");
      } catch (e) {
        setOfferError(
          e instanceof Error ? e.message : "Could not open this offer",
        );
      } finally {
        setOfferLoadingId(null);
      }
    },
    [userId],
  );

  return (
    <>
      <PageHeading eyebrow="discover" title="Context, not substitution.">
        Cohorts can help a new routine cold start, but they never replace your
        own record. A cohort signal is background reading — the only source of
        a personal verdict is still your own history.
      </PageHeading>

      {!isPremium ? (
        <Reveal>
          <PaywallCard
            title="Cohort signals are a Premium feature"
            body="Cohort context only ships once enough consenting people share the same product outcome — small cohorts stay hidden below the minimum sample size. Access to the feature is a subscription; the signals themselves are never for sale and never bought into."
            onUnlock={handleUnlock}
            pending={upgrading}
          />
          {upgradeError && (
            <div className="mt-4">
              <Notice tone="error">{upgradeError}</Notice>
            </div>
          )}
        </Reveal>
      ) : cohorts === null && offers === null ? (
        <DiscoverSkeleton />
      ) : (
        <>
          <Reveal delay={0.04}>
            <Card>
              <CardTitle
                action={
                  <span className="flex items-center gap-1.5 text-[12px] text-subtle">
                    <SparkIcon size={15} /> minimum sample required
                  </span>
                }
              >
                Cohort signals
              </CardTitle>
              {cohortError && (
                <div className="mb-4">
                  <Notice tone="error">{cohortError}</Notice>
                </div>
              )}
              {cohorts && cohorts.length === 0 && !cohortError ? (
                <EmptyState
                  title="No cohort has reached the minimum sample size yet"
                  body="Cohort context only appears once enough consenting people share an outcome for the same product. Until then there is nothing reliable to show."
                />
              ) : cohorts && cohorts.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                  {cohorts.map((c, i) => {
                    const effect = formatEffect(c.average_effect);
                    return (
                      <Card key={i} as="article" className="bg-surface-2" interactive>
                        <div className="flex items-start justify-between gap-3">
                          <p className="font-bold">{c.name}</p>
                          <Tag>{c.category}</Tag>
                        </div>
                        <div className="mt-3 flex items-baseline gap-2">
                          <span
                            className={cx(
                              "tnum display-sm text-[22px]",
                              effect.improvement ? "text-useful" : "text-investigate",
                            )}
                          >
                            {effect.text}
                          </span>
                          <span className="text-[12px] font-semibold text-muted">
                            average {effect.improvement ? "improvement" : "increase"}
                          </span>
                        </div>
                        <p className="mt-3 text-[12px] text-subtle">
                          Based on {c.sample_size} consenting{" "}
                          {c.sample_size === 1 ? "profile" : "profiles"} meeting the
                          minimum sample size.
                        </p>
                      </Card>
                    );
                  })}
                </div>
              ) : null}
            </Card>
          </Reveal>

          <Reveal delay={0.08} className="mt-4">
            <Card>
              <CardTitle
                action={
                  <span className="flex items-center gap-1.5 text-[12px] text-subtle">
                    <DiscoverIcon size={15} /> affiliate only
                  </span>
                }
              >
                Where to buy
              </CardTitle>
              {offerError && (
                <div className="mb-4">
                  <Notice tone="error">{offerError}</Notice>
                </div>
              )}
              {offers && offers.length === 0 && !offerError ? (
                <EmptyState
                  title="No affiliate offers available"
                  body="Offers only ever come from disclosed affiliate relationships. Placement is never purchased, so there is nothing to show until a merchant link exists."
                />
              ) : offers && offers.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                  {offers.map((o) => (
                    <Card key={o.id} as="article" className="bg-surface-2">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-bold">{o.product_name}</p>
                          <p className="text-[13px] text-muted">{o.merchant}</p>
                        </div>
                        {o.price_cents != null && (
                          <p className="tnum display-sm text-[18px]">
                            {formatPrice(o.price_cents, o.currency ?? "USD")}
                          </p>
                        )}
                      </div>
                      <p className="mt-3 text-[12px] leading-relaxed text-subtle">
                        Affiliate link &mdash; we may earn a commission. This does
                        not affect your verdicts.
                      </p>
                      <Button
                        className="mt-4"
                        size="sm"
                        variant="secondary"
                        loading={offerLoadingId === o.id}
                        onClick={() => handleOfferClick(o.id)}
                      >
                        Visit merchant
                      </Button>
                    </Card>
                  ))}
                </div>
              ) : null}
            </Card>
          </Reveal>

          <Reveal delay={0.12} className="mt-4">
            <Card className="bg-surface-2">
              <p className="text-[13px] leading-relaxed text-muted">
                No product or merchant can pay for placement here, and cohort
                signals never change a personal verdict. Your verdicts are
                computed only from your own capture history.
              </p>
            </Card>
          </Reveal>
        </>
      )}
    </>
  );
}

function DiscoverSkeleton() {
  return (
    <>
      <Skeleton className="h-[220px] rounded-[var(--radius-lg)]" />
      <div className="mt-4">
        <Skeleton className="h-[160px] rounded-[var(--radius-lg)]" />
      </div>
    </>
  );
}
